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
import kotlin.math.max



interface RateLimiter {
    
    /**
     * Return the number of permits available for the given key,
     * without consuming any of them.
     */
    fun getAvailablePermits(key: String = ""): UInt
    
    /**
     * Request and consume the given number of permits for the given key.
     *
     * @return The number of permits that were actually consumed.
     * @param partial If true, the request will be partially fulfilled if there are not enough permits available.
     */
    fun requestPermits(key: String = "", permits: UInt = 1u, partial: Boolean = true): UInt
    
    /**
     * Consume the given number of permits for the given key.
     *
     * @throws IllegalStateException if there are not enough permits available.
     */
    fun requirePermits(key: String = "", permits: UInt = 1u)
    
    /**
     * Wait until the given number of permits are available for the given key, then consume them.
     */
    suspend fun waitForPermits(key: String = "", permits: UInt = 1u)
}



@Serializable
data class TokenBucketRateLimiterConfig(
    val bufferSize: UInt,
    val refillRate: Double,
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

/**
 * A rate limiter that uses the [token bucket algorithm](https://en.wikipedia.org/wiki/Token_bucket).
 */
class TokenBucketRateLimiter(
    private val config: TokenBucketRateLimiterConfig,
    private val clock: () -> Instant = { Instant.now() },
    private val onStateChange: (TokenBucketRateLimiterState) -> Unit = {},
    initialState: TokenBucketRateLimiterState? = null,
) : RateLimiter {
    
    private val stateMutex = ReentrantLock()
    var state = (initialState ?: TokenBucketRateLimiterState())
        .also(onStateChange)
    
    override fun getAvailablePermits(key: String): UInt =
        freezeFrame {
            val consumedBefore = cleanUpAndGetState(key)?.permitsConsumed ?: 0u
            val available = config.bufferSize - consumedBefore
            return@freezeFrame available
        }

    override fun requestPermits(key: String, permits: UInt, partial: Boolean): UInt =
        freezeFrame {
            val stateBefore = cleanUpAndGetState(key)
            val consumedBefore = stateBefore?.permitsConsumed ?: 0u
            val available = config.bufferSize - consumedBefore
            val consumedNow = when {
                permits <= available -> permits
                partial -> available
                else -> 0u
            }
            updateState(key, TokenBucketRateLimiterState.KeyState(
                lastRefill = stateBefore?.lastRefill ?: now,
                permitsConsumed = consumedBefore + consumedNow,
            ))
            return@freezeFrame consumedNow
        }
    
    context(FreezeFrame)
    private fun cleanUpAndGetState(key: String): TokenBucketRateLimiterState.KeyState? {
        if (!cleanUpIfNeeded()) {
            state.perKey[key]?.let { keyState ->
                updateState(key, refill(keyState))
            }
        }
        return state.perKey[key]
    }
    
    context(FreezeFrame)
    private fun refill(keyState: TokenBucketRateLimiterState.KeyState): TokenBucketRateLimiterState.KeyState? {
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
                keyState.copy(
                    permitsConsumed = keyState.permitsConsumed - permitsToRefill,
                    lastRefill = now,
                )
            }
        }
        return ns
    }
    
    override fun requirePermits(key: String, permits: UInt) {
        val available = requestPermits(key, permits, partial = false)
        if (available < permits) {
            error("Rate limited")
        }
    }
    
    override suspend fun waitForPermits(key: String, permits: UInt) {
        var waitTime = 0.0 // in seconds
        while (true) {
            when (val available = requestPermits(key, permits, partial = false)) {
                permits -> break
                0u -> {
                    val remainingRefillTime = if (waitTime > 0) 0.0 else max(0.0, 1.0 / config.refillRate - waitTime)
                    val requiredTime = (permits.toDouble() - available.toDouble()) / config.refillRate
                    waitTime += requiredTime + remainingRefillTime
                    delay((waitTime * 1000).toLong()) // convert to milliseconds
                }
                else -> error("Unexpected available permits: $available")
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
                val ns = refill(keyState)
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

private data class FreezeFrame(
    val now: Instant,
)
