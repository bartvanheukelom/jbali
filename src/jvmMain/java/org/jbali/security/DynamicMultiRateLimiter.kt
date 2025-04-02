package org.jbali.security

import org.jbali.events.EventDelegate
import org.jbali.events.Observable
import java.time.Instant
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

class DynamicMultiRateLimiter<O>(
    private val rules: Observable<List<MultiRateLimiter.Rule<O>>>,
    private val clock: () -> Instant = { Instant.now() }
) : OpRateLimiter<O>, AutoCloseable {
    
    val onMutationStarted by EventDelegate<String>()
    val onMutated by EventDelegate<MultiRateLimiter.MutationMetrics>()
    val onRuleEvaluated by EventDelegate<MultiRateLimiter.RuleEvaluation>()
    
    @Volatile
    private var limiter = MultiRateLimiter<O>(emptyList(), clock)
    
    private val lock = ReentrantReadWriteLock()
    
    // Use the write lock only here when updating the limiter.
    private val listener = rules.bind {
        lock.writeLock().withLock {
            val grants = limiter.grantHistory()
            limiter = MultiRateLimiter(it, clock).also {
                it.onMutationStarted.listen(onMutationStarted::dispatch)
                it.onMutated.listen(onMutated::dispatch)
                it.onRuleEvaluated.listen(onRuleEvaluated::dispatch)
                it.addGrantHistory(grants)
            }
        }
    }
    
    override fun close() {
        listener.detach()
    }
    
    fun grantHistory() = lock.readLock().withLock {
        limiter.grantHistory()
    }
    
    override fun getAvailablePermits(op: O) = lock.readLock().withLock {
        limiter.getAvailablePermits(op)
    }
    
    override fun requestPermits(op: O, permits: UInt, partial: Boolean) = lock.readLock().withLock {
        limiter.requestPermits(op, permits, partial)
    }
    
    override suspend fun waitForPermits(op: O, permits: UInt, onWait: () -> Unit) =
        lock.readLock().withLock { limiter }.waitForPermits(op, permits, onWait)
    
}
