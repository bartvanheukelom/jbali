package org.jbali.security

import kotlinx.serialization.Serializable
import org.jbali.collect.removeLast
import org.jbali.collect.removeWhile
import org.jbali.events.EventDelegate
import org.jbali.kotser.std.InstantSerializer
import org.jbali.util.logger
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
    
    data class RuleEvaluation(
        val name: String,
        val grouping: String,
        val rate: BurstRate,
        val requested: UInt,
        val available: UInt,
        val partial: Boolean = false,
    ) {
        val granted: UInt get() = when {
            requested <= available -> requested
            partial -> available
            else -> 0u
        }
    }
    
    val onRuleEvaluated by EventDelegate<RuleEvaluation>()
    
    data class Rule<O>(
        val name: String,
        val scope: (O) -> Boolean,
        val groupings: List<Grouping<O, *>>,
    )
    data class Grouping<O, G>(
        val name: String,
        val opGroup: (O) -> G,
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
        fun getAvailablePermits(ff: FreezeFrame, op: O, requested: UInt? = null, partial: Boolean? = null): UInt {
            
            ff.cullGrants()
            
//            log.info("getAvailablePermits $op, recent grants:\n${grants.toTableString()}")
            
            // Compute available permits for each grouping.
            val groupingAvailabilities: List<UInt> = rule.groupings.map { grouping ->
                val key = grouping.opGroup(op)
                // For each rate, count the grants in this grouping
                val rateAvailabilities: List<UInt> = grouping.rates.map { rate ->
                    val cutoff = ff.now.minus(rate.window)
//                    val windowGrants = grants.filter { grant ->
//                        grouping.opGroup(grant.op) == key && grant.ts > cutoff
//                    }
                    val windowGrants = grants.asReversed().asSequence()
                        .takeWhile { it.ts > cutoff }
                        .filter { grouping.opGroup(it.op) == key }
//                        .toList() // enable if enabling the below logs
//                    log.info("For grouping ${grouping.name}, rate $rate, grants in window:\n${windowGrants.toTableString()}")
                    
                    val used = windowGrants.sumOf { it.granted }
                    val available = maxOf(0u, rate.permits - used)
                    if (requested != null) {
                        onRuleEvaluated.dispatch(RuleEvaluation(
                            name = rule.name,
                            grouping = grouping.name,
                            rate = rate,
                            requested = requested,
                            available = available,
                            partial = partial!!,
                        ))
                    }
                    
//                    log.info("${windowGrants.size} grants issued $used permits, $available available")
                    available
                }
                // This grouping is as restrictive as its lowest available rate.
                rateAvailabilities.minOrNull() ?: 0u
            }
            // The rule is as restrictive as its most constrained grouping.
            return groupingAvailabilities.minOrNull() ?: UInt.MAX_VALUE
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
        mutate {
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
        mutate { // not actually mutating, just locking
            ruleHandlers
                .flatMap { it.grants }
                .sortedBy { it.ts }
                // deduplicate by identity
                .distinctBy { it }
        }
    
    private inline fun <T> mutate(block: FreezeFrame.() -> T): T =
        synchronized(this) {
            with(FreezeFrame(clock())) {
                block()
            }
        }
    
    
    /**
     * Returns the number of permits available for an operation.
     * If no rules apply, returns UInt.MAX_VALUE (i.e. effectively unlimited).
     */
    override fun getAvailablePermits(op: O): UInt =
        mutate {
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
        mutate {
            
            // Determine which rules apply.
            val matchingHandlers = ruleHandlers.filter { it.rule.scope(op) }
            
            // If no rule applies, consider the operation unlimited.
            val overallAvailable: UInt = matchingHandlers
                .minOfOrNull { it.getAvailablePermits(
                    ff = this, op = op,
                    requested = permits, partial = partial,
                ) } ?: UInt.MAX_VALUE
            
            // Decide how many permits to grant.
            val granted: UInt = when {
                overallAvailable >= permits -> permits
                partial -> overallAvailable
                else -> 0u
            }
            
            // If we are granting any permits, update each matching rule’s state.
            if (granted > 0u) {
                val grant = MultiRateLimiterGrant(
                    ts = now,
                    op = op,
                    requested = permits,
                    available = overallAvailable,
                    granted = granted,
                )
                matchingHandlers.forEach { handler ->
                    handler.grants.add(grant)
                }
                
                return PermitsImpl(
                    permits, overallAvailable,
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
                    permits, overallAvailable,
                    0u, null,
                    giveBackImpl = { p -> require(p == 0u) { "Can't give back $p permits, none were granted" } },
                )
            }
        }
    }
    
    override suspend fun waitForPermits(op: O, permits: UInt, onWait: () -> Unit): Permits {
        TODO("Not yet implemented")
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
