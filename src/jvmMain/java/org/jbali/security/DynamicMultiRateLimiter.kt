package org.jbali.security

import org.jbali.events.EventDelegate
import org.jbali.events.Observable
import java.time.Instant

class DynamicMultiRateLimiter<O>(
    private val rules: Observable<List<MultiRateLimiter.Rule<O>>>,
    private val clock: () -> Instant = { Instant.now() }
) : OpRateLimiter<O>, AutoCloseable {
    
    val onRuleEvaluated by EventDelegate<MultiRateLimiter.RuleEvaluation>()
    
    @Volatile
    private var limiter = MultiRateLimiter<O>(emptyList(), clock)
    
    private val listener = rules.bind {
        synchronized(this) {
            val grants = limiter.grantHistory()
            limiter = MultiRateLimiter(it, clock).also {
                it.onRuleEvaluated.listen(onRuleEvaluated::dispatch)
                it.addGrantHistory(grants)
            }
        }
    }
    
    override fun close() {
        listener.detach()
    }
    
    override fun getAvailablePermits(op: O) = synchronized(this) {
        limiter.getAvailablePermits(op)
    }
    
    override fun requestPermits(op: O, permits: UInt, partial: Boolean) = synchronized(this) {
        limiter.requestPermits(op, permits, partial)
    }
    
    override suspend fun waitForPermits(op: O, permits: UInt, onWait: () -> Unit) =
        synchronized(this) { limiter }.waitForPermits(op, permits, onWait)
    
}
