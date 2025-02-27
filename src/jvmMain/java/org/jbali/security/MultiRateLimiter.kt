package org.jbali.security

import java.time.Duration
import java.time.Instant


/**
 * A rate limiter that can apply multiple rate limits to operations in a single permit check.
 * For example, an operation `HTTP GET /api/foobars` could be subject to:
 *
 * - `HTTP GET /api*`, globally, 3/second
 * - `HTTP GET /api`, globally, 60/minute
 * - `HTTP GET /api`, per IP, 10/second
 * - `HTTP GET /api/foobars`, globally, 1/second
 * - `HTTP GET /api/foobars`, per user, 5/second
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
 *                     BurstRate( 3u, Duration.ofSeconds( 1)), // 3/second
 *                     BurstRate(60u, Duration.ofSeconds(60)), // 60/minute
 *                 ),
 *                 // per IP
 *                 MultiRateLimiter.Grouping(name = "ip", opGroup = { op: HTTPOp -> op.ip }, rates = listOf(
 *                     BurstRate(10u, Duration.ofSeconds(1)),
 *                 ),
 *             ),
 *         ),
 *         // HTTP GET /api/foobars
 *         MultiRateLimiter.Rule(
 *             name = "api/foobars",
 *             scope = { it.method == HTTPMeth.GET && it.path == "/api/foobars" },
 *             groupings = listOf(
 *                 MultiRateLimiter.Grouping(name = "global", opGroup = { "global" }, rates = listOf(
 *                     BurstRate(1u, Duration.ofSeconds(1)),
 *                 ),
 *                 MultiRateLimiter.Grouping(name = "user", opGroup = { it.user }, rates = listOf(
 *                     BurstRate(5u, Duration.ofSeconds(1)),
 *                 ),
 *             ),
 *         ),
 *     ),
 * )
 * ```
 */
class MultiRateLimiter<O>(
    private val rules: List<Rule<O, *>>,
    private val clock: () -> Instant = { Instant.now() },
) : OpRateLimiter<O> {
    
    data class Rule<O, G>(
        val name: String,
        val scope: (O) -> Boolean,
        val groupings: List<Grouping<O, G>>,
    )
    data class Grouping<O, G>(
        val name: String,
        val opGroup: (O) -> G,
        val rates: List<BurstRate>,
    )
    
    // state container for a rule
    private data class RuleState<O>(
        val grants: List<Pair<Instant, O>> = emptyList(),
    )
    
    private inner class RuleHandler(
        val rule: Rule<O, *>,
    ) {
        // The longest time window for any rate in this rule (used for culling old grants)
        val longestWindow: Duration = rule.groupings
            .flatMap { it.rates }
            .maxOfOrNull { it.window } ?: Duration.ZERO
        
        // This state is mutated inside synchronized blocks.
        var state = RuleState<O>()
        
        /**
         * For each grouping in the rule, count how many grants (i.e. previously granted permits)
         * have been recorded for the same group (as defined by opGroup) and are within the rate’s window.
         * The available permits for each rate is the burst size minus the count, and the grouping’s availability
         * is the minimum over its rates.
         *
         * The rule’s available permits is the minimum over its groupings.
         */
        fun getAvailablePermits(op: O): UInt {
            val now = clock()
            
            // Cull any expired grants. Since longestWindow is the maximum of all group windows,
            // any grant older than now - longestWindow is irrelevant.
            state = state.copy(
                grants = state.grants.filter { it.first >= now.minus(longestWindow) }
            )
            
            // Compute available permits for each grouping.
            val groupingAvailabilities: List<UInt> = rule.groupings.map { grouping ->
                val key = grouping.opGroup(op)
                // For each rate, count the grants in this grouping
                val rateAvailabilities: List<UInt> = grouping.rates.map { rate ->
                    val count = state.grants.count { grant ->
                        grouping.opGroup(grant.second) == key && grant.first >= now.minus(rate.window)
                    }
                    if (count >= rate.permits.toInt()) 0u else (rate.permits - count.toUInt())
                }
                // This grouping is as restrictive as its lowest available rate.
                rateAvailabilities.minOrNull() ?: 0u
            }
            // The rule is as restrictive as its most constrained grouping.
            return groupingAvailabilities.minOrNull() ?: UInt.MAX_VALUE
        }
    }
    
    // Prepare a RuleHandler for each rule.
    private val ruleHandlers = rules.map { RuleHandler(it) }
    
    /**
     * Returns the number of permits available for an operation.
     * If no rules apply, returns UInt.MAX_VALUE (i.e. effectively unlimited).
     */
    override fun getAvailablePermits(op: O): UInt {
        synchronized(this) {
            val matchingHandlers = ruleHandlers.filter { it.rule.scope(op) }
            if (matchingHandlers.isEmpty()) {
                return UInt.MAX_VALUE
            }
            return matchingHandlers.map { it.getAvailablePermits(op) }.minOrNull() ?: 0u
        }
    }
    
    /**
     * Attempts to request [permits] for the given operation.
     * In non-partial mode, if there aren’t enough permits available, nothing is granted.
     * In partial mode, grants as many as are available (if > 0).
     *
     * The state is updated in all matching rules only if some permits are granted.
     */
    override fun requestPermits(op: O, permits: UInt, partial: Boolean): Permits {
        synchronized(this) {
            val now = clock()
            
            // Determine which rules apply.
            val matchingHandlers = ruleHandlers.filter { it.rule.scope(op) }
            
            // If no rule applies, consider the operation unlimited.
            val overallAvailable: UInt = if (matchingHandlers.isEmpty()) {
                UInt.MAX_VALUE
            } else {
                matchingHandlers.map { it.getAvailablePermits(op) }.minOrNull() ?: 0u
            }
            
            // Decide how many permits to grant.
            val granted: UInt = when {
                !partial && overallAvailable < permits -> 0u
                partial -> if (overallAvailable > 0u) minOf(permits, overallAvailable) else 0u
                else -> permits
            }
            
            // If we are granting any permits, update each matching rule’s state.
            if (granted > 0u) {
                matchingHandlers.forEach { handler ->
                    val newGrants = List(granted.toInt()) { Pair(now, op) }
                    handler.state = handler.state.copy(grants = handler.state.grants + newGrants)
                }
            }
            
            // If no permits were granted, try to predict when one might become available.
            val availableAt: Instant? = if (granted == 0u && matchingHandlers.isNotEmpty()) {
                // For each matching handler, check each grouping and rate.
                var predicted = now
                matchingHandlers.forEach { handler ->
                    handler.rule.groupings.forEach { grouping ->
                        val key = grouping.opGroup(op)
                        grouping.rates.forEach { rate ->
                            // Only consider grants in this grouping and within this rate's window.
                            val relevantGrants = handler.state.grants.filter { grant ->
                                grouping.opGroup(grant.second) == key &&
                                        grant.first >= now.minus(rate.window)
                            }
                            if (relevantGrants.size >= rate.permits.toInt()) {
                                // Next permit becomes available when the oldest grant expires.
                                val oldest = relevantGrants.minByOrNull { it.first }!!.first
                                val candidate = oldest.plus(rate.window)
                                if (candidate > predicted) {
                                    predicted = candidate
                                }
                            }
                        }
                    }
                }
                if (predicted == now) null else predicted
            } else {
                null
            }
            
            // Define the "give back" behavior: when a consumer returns unused permits,
            // remove them from each matching rule’s state.
            val giveBackImpl: (UInt) -> Unit = { p ->
                matchingHandlers.forEach { handler ->
                    var pToRemove = p.toInt()
                    // Rebuild the grant list excluding up to p matching grants.
                    val newList = mutableListOf<Pair<Instant, O>>()
                    for (grant in handler.state.grants) {
                        if (pToRemove > 0 && grant.second == op) {
                            pToRemove--
                        } else {
                            newList.add(grant)
                        }
                    }
                    handler.state = handler.state.copy(grants = newList)
                }
            }
            
            return PermitsImpl(
                requested = permits,
                available = overallAvailable,
                granted = granted,
                availableAt = availableAt,
                giveBackImpl = giveBackImpl
            )
        }
    }
    
    override suspend fun waitForPermits(op: O, permits: UInt, onWait: () -> Unit): Permits {
        TODO("Not yet implemented")
    }
}
