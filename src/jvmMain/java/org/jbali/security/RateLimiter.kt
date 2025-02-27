@file:OptIn(ExperimentalContracts::class, ExperimentalContracts::class)

package org.jbali.security

import java.time.Instant
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


interface OpRateLimiter<O> {
    
    /**
     * Return the number of permits available for the given key,
     * without consuming any of them.
     */
    fun getAvailablePermits(op: O): UInt
    
    /**
     * Request the given number of permits for the given key.
     * Returns immediately, without blocking or suspending, whether or not the permits are available.
     *
     * @param partial If true, the request will be partially fulfilled if there are some permits available, but fewer than requested.
     *                If false, null will be returned in that case.
     * @return The granted permits, or `null` if none were. Note that `null` was chosen over 0 to slightly reduce the chance
     *         of accidentally using this function instead of the intended [requirePermits], and to force the caller to
     *         consider whether they should do anything at all with 0 permits. TODO but not. fix doc.
     */
    fun requestPermits(op: O, permits: UInt = 1u, partial: Boolean = true): Permits
    
    /**
     * Get the given number of permits for the given key.
     *
     * @throws RateLimitExceededException if there are not enough permits available.
     */
    fun requirePermits(op: O, permits: UInt = 1u): Permits =
        requestPermits(op, permits, partial = false).also { res ->
            assert(res.requested == permits)
            if (res.granted < permits) {
                throw RateLimitExceededException("Rate limited, ${res.available} of requested ${res.requested} permits available")
            }
        }
    
    /**
     * Wait until the given number of permits are available for the given key, then consume them.
     * @param onWait Called once if and when the coroutine is about to be suspended waiting for permits. Not called if no waiting is needed.
     */
    suspend fun waitForPermits(op: O, permits: UInt = 1u, onWait: () -> Unit = {}): Permits
    
}


interface RateLimiter : OpRateLimiter<String> {
    fun getAvailablePermits(): UInt = getAvailablePermits("")
    fun requestPermits(permits: UInt = 1u, partial: Boolean = true): Permits = requestPermits("", permits, partial)
    fun requirePermits(permits: UInt = 1u): Permits = requirePermits("", permits)
    suspend fun waitForPermits(permits: UInt = 1u, onWait: () -> Unit = {}): Permits = waitForPermits("", permits, onWait)
}


/**
 * Contains information about permits requested and granted,
 * and allows returning unused permits to the rate limiter.
 *
 * Thread-safety: unless specified otherwise, an object of this type is not safe to use concurrently from multiple threads,
 * but multiple objects granted by the same rate limiter can be used concurrently if that rate limiter can.
 */
interface Permits {
    
    /**
     * The number of permits requested.
     */
    val requested: UInt
    
    /**
     * The number of permits available at the time of the request, up to the number [requested], whether they have been granted or not.
     */
    val available: UInt
    
    /**
     * The total number of permits granted. Can be fewer than requested if [RateLimiter.requestPermits] was used.
     */
    val granted: UInt
    
    /**
     * If not enough permits are available and none were granted, this may predict when they will become available.
     */
    val availableAt: Instant?
    
    
    // --- giving back --- //
    
    /**
     * The number of permits given back with [giveBack].
     */
    val returned: UInt
    
    val unused: UInt get() = granted - returned
    
    /**
     * Return the given number of permits to the rate limiter, unused. Other consumers will become able to acquire them
     * immediately, without waiting. Use this if you didn't end up using any or all of the permits you requested,
     * e.g. if you have a limit on posting X messages per hour, but failed to post a message.
     *
     * @param p The number of permits to return. Defaults to all of them.
     * @throws IllegalArgumentException if [unused] is greater than [unused].
     */
    fun giveBack(p: UInt = unused)
    
    companion object {
        /**
         * Create a grant of the given number of [Permits] out of thin air, which can be "given back" even though they
         * came from nowhere.
         */
        fun fabricate(granted: UInt = 1u): Permits = PermitsImpl(granted, granted, granted) {
            // thanks for giving back these permits, we'll store them in the cylindrical file cabinet
        }
    }
    
}

/**
 * Runs [block] and returns its result. If the block throws, the permits are given back.
 */
inline fun <T> Permits.consume(p: UInt = unused, block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return try {
        block()
    } catch (e: Throwable) {
        try {
            giveBack(p)
        } catch (e2: Throwable) {
            e.addSuppressed(e2)
        }
        throw e
    }
}


class RateLimitExceededException(message: String) : RuntimeException(message)



// ------------------------ NOP IMPLEMENTATION ------------------------ //

/**
 * A rate "limiter" that doesn't actually limit anything. Useful for unit tests.
 */
class RateUnlimiter : RateLimiter {
    override fun getAvailablePermits(op: String): UInt = UInt.MAX_VALUE
    override fun requestPermits(op: String, permits: UInt, partial: Boolean): Permits =
        PermitsImpl(permits, permits, permits) {
            // thanks yo
        }
    override suspend fun waitForPermits(op: String, permits: UInt, onWait: () -> Unit) = requestPermits(op, permits)
}

internal class PermitsImpl(
    override val requested: UInt,
    override val available: UInt,
    override val granted: UInt,
    override val availableAt: Instant? = null,
    private val giveBackImpl: (UInt) -> Unit,
) : Permits {
    
    override fun toString() = "Permits(requested=$requested, available=$available, granted=$granted, returned=$returned)"
    
    override var returned: UInt = 0u
        private set
    override fun giveBack(p: UInt) {
        require(p <= unused) { "Can't give back $p permits, only $unused unused, of $granted granted" }
        giveBackImpl(p)
        returned += p
    }
}
