package org.jbali.security

import arrow.core.left
import arrow.core.right
import kotlinx.serialization.Serializable
import org.jbali.arrow.getOrRethrow
import org.jbali.collect.removeLast
import org.jbali.collect.removeWhile
import org.jbali.events.EventDelegate
import org.jbali.kotser.std.InstantSerializer
import org.jbali.util.NanoDuration
import org.jbali.util.NanoTime
import org.jbali.util.logger
import org.jbali.util.measure
import java.time.Duration
import java.time.Instant
import java.util.*


/**
 * A rate limiter that can apply multiple rate limits to operations in a single permit check.
 * For example, an operation `HTTP GET /api/foobars` could be subject to:
 *
 * - `HTTP GET /api`, globally, 6/second
 * - `HTTP GET /api`, globally, 60/minute
 * - `HTTP GET /api`, per IP, 3/second
 * - `HTTP GET /api/foobars`, globally, 5/second
 * - `HTTP GET /api/foobars`, per user, 1/second
 *
 * A permit will be granted if all of these limits allow it.
 * If one of them does not, the permit is denied, and the operation is not counted against any limit that were already evaluated.
 *
 * Example code to configure the rules above:
 *
 * ```
 * val rateLimiter = MultiRateLimiter<HTTPOp>(
 *     rules = listOf(
 *         // HTTP GET /api*
 *         MultiRateLimiter.Rule(
 *             name = "api",
 *             scope = { op: HTTPOp -> op.method == HTTPMeth.GET && op.path.startsWith("/api") },
 *             groupings = listOf(
 *                 // globally
 *                 MultiRateLimiter.Grouping(name = "global", opGroup = { "global" }, rates = listOf(
 *                     BurstRate( 6u, Duration.ofSeconds( 1)),
 *                     BurstRate(60u, Duration.ofSeconds(60)),
 *                 ),
 *                 // per IP
 *                 MultiRateLimiter.Grouping(name = "ip", opGroup = { op: HTTPOp -> op.ip }, rates = listOf(
 *                     BurstRate( 3u, Duration.ofSeconds(1)),
 *                 ),
 *             ),
 *         ),
 *         // HTTP GET /api/foobars
 *         MultiRateLimiter.Rule(
 *             name = "api/foobars",
 *             scope = { it.method == HTTPMeth.GET && it.path == "/api/foobars" },
 *             groupings = listOf(
 *                 MultiRateLimiter.Grouping(name = "global", opGroup = { "global" }, rates = listOf(
 *                     BurstRate(5u, Duration.ofSeconds(1)),
 *                 ),
 *                 MultiRateLimiter.Grouping(name = "user", opGroup = { it.user }, rates = listOf(
 *                     BurstRate(1u, Duration.ofSeconds(1)),
 *                 ),
 *             ),
 *         ),
 *     ),
 * )
 * ```
 */
class MultiRateLimiter<O>(
    private val rules: List<Rule<O>>,
    private val clock: () -> Instant = { Instant.now() },
) : OpRateLimiter<O> {
    
    private val log = logger<MultiRateLimiter<*>>()
    
    data class MutationMetrics(
        val operation: String,
        val waitedFor: NanoDuration,
        val ranFor: NanoDuration,
        val exception: Throwable? = null,
    )
    
    enum class RuleDepth {
        All, Rule, Grouping, Rate;
        
        val lowerName get() = when (this) {
            All -> "all"
            Rule -> "rule"
            Grouping -> "grouping"
            Rate -> "rate"
        }
    }
    
    data class RuleEvaluation(
        val duration: NanoDuration,
        val name: String? = null,
        val grouping: String? = null,
        val rate: BurstRate? = null,
        val requested: UInt,
        val available: UInt,
        val partial: Boolean = false,
    ) {
        
        val depth: RuleDepth get() =
            when (name) {
                null -> when (grouping) {
                    null -> when (rate) {
                        null -> RuleDepth.All
                        else -> null
                    }
                    else -> null
                }
                else -> when (grouping) {
                    null -> when (rate) {
                        null -> RuleDepth.Rule // aka name
                        else -> null
                    }
                    else -> when (rate) {
                        null -> RuleDepth.Grouping
                        else -> RuleDepth.Rate
                    }
                }
            } ?: throw IllegalArgumentException("Invalid combination of name, grouping, and rate")
        
        init {
            depth // check for invalid combinations
        }
        
        val granted: UInt get() = when {
            requested <= available -> requested
            partial -> available
            else -> 0u
        }
    }
    
    val onMutated by EventDelegate<MutationMetrics>()
    val onRuleEvaluated by EventDelegate<RuleEvaluation>()
    
    data class Rule<O>(
        val name: String,
        val scope: (O) -> Boolean,
        val groupings: List<Grouping<O, *>>,
    )
    data class Grouping<O, G>(
        val name: String,
        /**
         * Function that extracts the group key from an operation.
         * For a global grouping, use `{ Unit }`, which is optimized.
         * Returning `Unit` for some ops and not others (e.g. under return type `Any`)
         * results in undefined behaviour.
         */
        val opGroup: ((O) -> G),
        val rates: List<BurstRate>,
    )
    
    private inner class RuleHandler(
        val rule: Rule<O>,
    ) {
        // The longest time window for any rate in this rule (used for culling old grants)
        val longestWindow: Duration = rule.groupings
            .flatMap { it.rates }
            .maxOfOrNull { it.window } ?: Duration.ZERO
        
        // This state is mutated inside synchronized blocks.
        val grants: LinkedList<MultiRateLimiterGrant<O>> = LinkedList()
        
        /**
         * For each grouping in the rule, count how many grants (i.e. previously granted permits)
         * have been recorded for the same group (as defined by opGroup) and are within the rate’s window.
         * The available permits for each rate is the burst size minus the count, and the grouping’s availability
         * is the minimum over its rates.
         *
         * The rule’s available permits is the minimum over its groupings.
         */
        fun getAvailablePermits(ff: FreezeFrame, op: O, requested: UInt? = null, partial: Boolean? = null): UInt =
            ruleEval(requested, partial, name = rule.name) {
                
                ff.cullGrants()
                
    //            log.info("getAvailablePermits $op, recent grants:\n${grants.toTableString()}")
                
                // Compute available permits for each grouping.
                val groupingAvailabilities: List<UInt> = rule.groupings.map { grouping ->
                    ruleEval(requested, partial, name = rule.name, grouping = grouping.name) {
                        
                        val key = grouping.opGroup(op)
                        // For each rate, count the grants in this grouping
                        val rateAvailabilities: List<UInt> = grouping.rates.map { rate ->
                            val available = ruleEval(requested, partial, name = rule.name, grouping = grouping.name, rate = rate) {
                                val cutoff = ff.now.minus(rate.window)
            //                    val windowGrants = grants.filter { grant ->
            //                        grouping.opGroup(grant.op) == key && grant.ts > cutoff
            //                    }
                                var windowGrants = grants.asReversed().asSequence()
                                    .takeWhile { it.ts > cutoff }
            //                        .toList() // enable if enabling the below logs
                                if (key != Unit) {
                                    windowGrants = windowGrants.filter { g ->
                                        grouping.opGroup(g.op) == key
                                    }
                                }
            //                    log.info("For grouping ${grouping.name}, rate $rate, grants in window:\n${windowGrants.toTableString()}")
                                
                                val used = windowGrants.sumOf { it.granted }
                                maxOf(0u, rate.permits - used)
                            }
        //                    log.info("${windowGrants.size} grants issued $used permits, $available available")
                            available
                        }
                        // This grouping is as restrictive as its lowest available rate.
                        rateAvailabilities.minOrNull()
                    } ?: 0u
                }
                // The rule is as restrictive as its most constrained grouping.
                groupingAvailabilities.minOrNull() ?: UInt.MAX_VALUE
            }
        
        private fun FreezeFrame.cullGrants() {
            // Cull any expired grants. Since longestWindow is the maximum of all group windows,
            // any grant older than now - longestWindow is irrelevant.
            val forgetBefore = now.minus(longestWindow)
            grants.removeWhile { it.ts < forgetBefore }
        }
        
        fun sort() {
            grants.sortBy { it.ts }
        }
        
    }
    
    // Prepare a RuleHandler for each rule.
    private val ruleHandlers = rules.map { RuleHandler(it) }
    
    
    /**
     * Add the given grant records to the state.
     * Meant for restoring from persistence or unit tests.
     */
    fun addGrantHistory(grants: List<MultiRateLimiterGrant<O>>) {
        mutate("addGrantHistory") {
            var added = false
            for (grant in grants) {
                for (rule in ruleHandlers) {
                    if (rule.rule.scope(grant.op)) {
                        rule.grants.add(grant)
                        added = true
                    }
                }
            }
            if (added) {
                // Cull old grants to keep the state size manageable.
                ruleHandlers.forEach { it.sort() }
            }
        }
    }
    
    /**
     * Returns a list of all grant records in the state.
     * Meant for persistence or debugging.
     */
    fun grantHistory(): List<MultiRateLimiterGrant<O>> =
        mutate("grantHistory") { // not actually mutating, just locking
            ruleHandlers
                .flatMap { it.grants }
                .sortedBy { it.ts }
                // deduplicate by identity
                .distinctBy { it }
        }
    
    private inline fun <T> mutate(operation: String, block: FreezeFrame.() -> T): T =
        if (onMutated.hasListeners()) {
            val tsPreLock = NanoTime.now()
            synchronized(this) {
                val tsPostLock = NanoTime.now()
                val res = try {
                    with(FreezeFrame(clock())) {
                        block()
                    }.right()
                } catch (e: Throwable) {
                    e.left()
                }
                onMutated.dispatch(MutationMetrics(
                    operation = operation,
                    waitedFor = NanoDuration.between(tsPreLock, tsPostLock),
                    ranFor = NanoDuration.since(tsPostLock),
                    exception = res.fold({ it }, { null }),
                ))
                res.getOrRethrow()
            }
        } else {
            synchronized(this) {
                with(FreezeFrame(clock())) {
                    block()
                }
            }
        }
    
    
    /**
     * Returns the number of permits available for an operation.
     * If no rules apply, returns UInt.MAX_VALUE (i.e. effectively unlimited).
     */
    override fun getAvailablePermits(op: O): UInt =
        mutate("getAvailablePermits") {
            ruleHandlers.filter { it.rule.scope(op) }
                .minOfOrNull { it.getAvailablePermits(this, op) } ?: UInt.MAX_VALUE
        }
    
    /**
     * Attempts to request [permits] for the given operation.
     * In non-partial mode, if there aren’t enough permits available, nothing is granted.
     * In partial mode, grants as many as are available (if > 0).
     *
     * The state is updated in all matching rules only if some permits are granted.
     */
    override fun requestPermits(op: O, permits: UInt, partial: Boolean): Permits {
        mutate("requestPermits") {
            
            // Determine which rules apply.
            val matchingHandlers = ruleHandlers.filter { it.rule.scope(op) }
            
            // If no rule applies, consider the operation unlimited.
            // TODO probably better to restructure, make no-rules its own branch
            val overallAvailable = ruleEval(permits, partial) {
                matchingHandlers.minOfOrNull { it.getAvailablePermits(
                    ff = this, op = op,
                    requested = permits, partial = partial,
                ) }
            }
            
            // Decide how many permits to grant.
            val granted: UInt = when {
                overallAvailable == null -> permits
                overallAvailable >= permits -> permits
                partial -> overallAvailable
                else -> 0u
            }
            val apparentlyAvailable = overallAvailable ?: UInt.MAX_VALUE
            
            // If we are granting any permits, update each matching rule’s state.
            if (granted > 0u) {
                val grant = MultiRateLimiterGrant(
                    ts = now,
                    op = op,
                    requested = permits,
                    available = apparentlyAvailable,
                    granted = granted,
                )
                matchingHandlers.forEach { handler ->
                    handler.grants.add(grant)
                }
                
                return PermitsImpl(
                    permits, apparentlyAvailable,
                    granted, null,
                    giveBackImpl = { p ->
                        require(p <= granted) { "Can't give back $p permits, only $granted granted" }
                        // we can only undo grants completely. partial returns are ignored.
                        if (p == granted) {
                            matchingHandlers.forEach { handler ->
                                // assume returns are done quickly so searching from the tail is faster
                                handler.grants.removeLast { it === grant }
                            }
                        }
                    },
                )
            } else {
                return PermitsImpl(
                    permits, apparentlyAvailable,
                    0u, null,
                    giveBackImpl = { p -> require(p == 0u) { "Can't give back $p permits, none were granted" } },
                )
            }
        }
    }
    
    override suspend fun waitForPermits(op: O, permits: UInt, onWait: () -> Unit): Permits {
        TODO("Not yet implemented")
    }
    
    private fun <A : UInt?> ruleEval(
        requested: UInt?,
        partial: Boolean?,
        
        name: String? = null,
        grouping: String? = null,
        rate: BurstRate? = null,
        
        eval: () -> A,
    ): A {
        if (requested != null) {
            val (available, duration) = NanoDuration.measure {
                eval()
            }
            available?.let {  // if no rules apply, we haven't evaluated anything
                onRuleEvaluated.dispatch(RuleEvaluation(
                    duration = duration,
                    name = name,
                    grouping = grouping,
                    rate = rate,
                    requested = requested,
                    available = available,
                    partial = partial!!,
                ))
            }
            return available
        } else {
            return eval()
        }
    }
    
    data class RuleFlat(
        val name: String,
        val grouping: String,
        val permits: UInt,
        val window: Duration,
    )
    
    companion object {
        fun rulesFlat(rules: List<Rule<*>>): Sequence<RuleFlat> =
            rules.asSequence().flatMap { rule ->
                rule.groupings.asSequence().flatMap { grouping ->
                    grouping.rates.asSequence().map { rate ->
                        RuleFlat(rule.name, grouping.name, rate.permits, rate.window)
                    }
                }
            }
    }
    
}


@Serializable
data class MultiRateLimiterGrant<O>(
    val ts: @Serializable(with = InstantSerializer::class) Instant,
    val op: O,
    val requested: UInt,
    val available: UInt,
    val granted: UInt,
)
