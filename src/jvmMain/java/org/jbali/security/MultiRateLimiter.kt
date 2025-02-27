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
    
    
    // state
    
    inner class RuleHandler(
        val rule: Rule<O, *>,
    ) {
        val longestWindow = rule.groupings
            .flatMap { it.rates }
            .maxOfOrNull { it.window } ?: Duration.ZERO
        private var state = RuleState<O>()
        
        fun getAvailablePermits(op: O): UInt {
        
        }
    }
    
    private data class RuleState<O>(
        val lastCull: Instant? = null,
        val grants: List<Pair<Instant, O>> = emptyList(),
    )
    
    private val ruleHandlers = rules.map { RuleHandler(it) }
    
    
    // operations
    
    override fun getAvailablePermits(op: O): UInt {
        TODO("Not yet implemented")
    }
    
    override fun requestPermits(op: O, permits: UInt, partial: Boolean): Permits {
        
        synchronized(this) {
            
            // find rules matching the operation
        
            // for each rule, for each grouping, get how many permits are available,
            // and find the lowest of them
            
            // if the lowest is enough, or partial is true and more than 0,
            // store a grant in each rule, and return permits.
            
        }
        
    }
    
    override suspend fun waitForPermits(op: O, permits: UInt, onWait: () -> Unit): Permits {
        TODO("Not yet implemented")
    }
}
