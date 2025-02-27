package org.jbali.security

import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.jbali.kotser.std.DurationSerializer
import org.jbali.kotser.std.InstantSerializer
import org.jbali.threeten.secondsDouble
import java.time.Duration
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A rate limiter that uses the [token bucket algorithm](https://en.wikipedia.org/wiki/Token_bucket).
 */
class TokenBucketRateLimiter(
    private val config: TokenBucketRateLimiterConfig,
    private val clock: () -> Instant = { Instant.now() },
    private val onStateChange: (TokenBucketRateLimiterState) -> Unit = {},
    initialState: TokenBucketRateLimiterState? = null,
) : RateLimiter {

//    companion object {
//        private val log = LoggerFactory.getLogger(TokenBucketRateLimiter::class.java)
//    }
    
    private val stateMutex = ReentrantLock()
    var state = (initialState ?: TokenBucketRateLimiterState())
        .also(onStateChange)
    
    override fun getAvailablePermits(key: String): UInt =
        freezeFrame {
            val consumedBefore = cleanUpAndGetState(key)?.permitsConsumed ?: 0u
            val available = config.bufferSize - consumedBefore
            return@freezeFrame available
        }

    override fun requestPermits(key: String, permits: UInt, partial: Boolean): Permits =
        freezeFrame {
            val stateBefore = cleanUpAndGetState(key)
            val consumedBefore = stateBefore?.permitsConsumed ?: 0u
            val available = config.bufferSize - consumedBefore
            val consumedNow = when {
                permits <= available -> permits
                partial -> available
                else -> 0u
            }
//            if (consumedNow > 0u) {
//                log.info("Consuming $consumedNow permits for key '$key'")
//            }
            updateState(key, TokenBucketRateLimiterState.KeyState(
                lastRefill = stateBefore?.lastRefill ?: now,
                permitsConsumed = consumedBefore + consumedNow,
            ))
            return@freezeFrame PermitsImpl(
                requested = permits,
                available = available.coerceAtMost(permits),
                granted = consumedNow,
                giveBackImpl = { returned ->
                    // TODO immediately resume suspended waitForPermits calls
                    updateState(key, TokenBucketRateLimiterState.KeyState(
                        lastRefill = now,
                        permitsConsumed = consumedBefore + consumedNow - returned,
                    ))
                },
            )
        }
    
    context(FreezeFrame)
    private fun cleanUpAndGetState(key: String): TokenBucketRateLimiterState.KeyState? {
        if (!cleanUpIfNeeded()) {
            state.perKey[key]?.let { keyState ->
                updateState(key, refill(key, keyState))
            }
        }
        return state.perKey[key]
    }
    
    context(FreezeFrame)
    private fun refill(key: String, keyState: TokenBucketRateLimiterState.KeyState): TokenBucketRateLimiterState.KeyState? {
        val timeElapsed = Duration.between(keyState.lastRefill, now).secondsDouble
        val permitsToRefill = (config.refillRate * timeElapsed).toUInt()
        val ns = when {
            permitsToRefill == 0u -> {
                keyState
            }
            permitsToRefill >= keyState.permitsConsumed -> {
                null
            }
            else -> {
//                log.info("Refilling $permitsToRefill permits for key '$key'. permitsConsumed:=${keyState.permitsConsumed}-${permitsToRefill}")
                keyState.copy(
                    permitsConsumed = keyState.permitsConsumed - permitsToRefill,
                    lastRefill = now,
                )
            }
        }
        return ns
    }
    
    override suspend fun waitForPermits(key: String, permits: UInt, onWait: () -> Unit): Permits {
        var onWaitCalled = false
        while (true) {
            val grant = requestPermits(key, permits, partial = false)
            when (grant.granted) {
                permits -> return grant
                0u -> {
//                    log.info("$available/$permits permits available")
                    
                    // TODO calculate ideal delay using last refill timestamp.
                    // TODO akshually, implement a proper queue. first come, first serve. lots of tasks waiting for 1 permit should not be able to starve somebody waiting for 4 permits.
                    val worstCaseNextPermitTime = 1.0 / config.refillRate
                    val fractionOfWorstCase = worstCaseNextPermitTime / 16.0
                    val nextCheckDelay = fractionOfWorstCase.coerceAtMost(1.0)
                    val delayMs = (nextCheckDelay * 1000.0).toLong()
                    
//                    log.info("checking again after $delayMs ms")
                    if (!onWaitCalled) {
                        onWait()
                        onWaitCalled = true
                    }
                    delay(delayMs)
                }
                else -> error("Unexpected available permits: $grant")
            }
        }
    }
    
    private fun setStateIfDifferent(ns: TokenBucketRateLimiterState) {
        if (state != ns) { // TODO optimize
            state = ns
            onStateChange(ns)
        }
    }
    
    context(FreezeFrame)
    private fun updateState(key: String, newState: TokenBucketRateLimiterState.KeyState?) {
        val perKey = state.perKey.toMutableMap().apply {
            if (newState == null) {
                remove(key)
            } else {
                put(key, newState)
            }
        }
        setStateIfDifferent(state.copy(
            perKey = perKey,
            lastCleanup = if (perKey.isEmpty()) null else state.lastCleanup ?: now,
        ))
    }
    
    context(FreezeFrame)
    private fun cleanUpIfNeeded(force: Boolean = false): Boolean {
        if (state.perKey.isNotEmpty() && (force || Duration.between(state.lastCleanup, now) >= config.cleanupInterval)) {
            
            // refill all keys
            val perKey = state.perKey.mapNotNull { (key, keyState) ->
                val ns = refill(key, keyState)
                if (ns == null) {
                    null
                } else {
                    key to ns
                }
            }.toMap()
            
            // if there are no keys left, clear the last cleanup time, resetting to the base state
            setStateIfDifferent(if (perKey.isEmpty()) {
                state.copy(perKey = emptyMap(), lastCleanup = null)
            } else {
                state.copy(perKey = perKey, lastCleanup = now)
            })
            
            return true
        } else {
            return false
        }
    }
    
    fun cleanUpNow(force: Boolean = false) =
        freezeFrame { cleanUpIfNeeded(force) }
    
    private inline fun <T> freezeFrame(body: context(FreezeFrame) () -> T) =
        stateMutex.withLock {
            with(FreezeFrame(clock())) {
                body(this)
            }
        }

}


@Serializable
data class TokenBucketRateLimiterConfig(
    val bufferSize: UInt,
    val refillRate: Double,
    // TODO doc that this cleanup also is responsible for refilling. if that's the case.
    //      or just make it not the case. if it remains the case, auto calculate the default
    //      such that waiting for 1 buffer to fill never takes longer than it should. or just auto refill if getting permits fails.
    val cleanupInterval: @Serializable(with = DurationSerializer::class) Duration = Duration.ofMinutes(1),
)


@Serializable
data class TokenBucketRateLimiterState(
    val perKey: Map<String, KeyState> = emptyMap(),
    val lastCleanup: @Serializable(with = InstantSerializer::class) Instant? = null,
) {
    
    init {
        require(perKey.isEmpty() == (lastCleanup == null)) {
            "perKey and lastCleanup must be both empty/null or both have values"
        }
    }
    
    @Serializable
    data class KeyState(
        val lastRefill: @Serializable(with = InstantSerializer::class) Instant,
        val permitsConsumed: UInt,
    )
}


private data class FreezeFrame(
    val now: Instant,
)
