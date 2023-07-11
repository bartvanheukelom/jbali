package org.jbali.random

import kotlinx.serialization.Serializable
import org.jbali.util.letRepeat
import kotlin.random.Random

/**
 * Immutable state snapshot of a random number generator, using Marsaglia's "xorwow" algorithm.
 * Cycles after 2^192 - 2^32 repetitions.
 * For more details, see Marsaglia, George (July 2003). "Xorshift RNGs". Journal of Statistical Software. 8 (14). doi:10.18637/jss.v008.i14
 * Available at https://www.jstatsoft.org/v08/i14/paper
 *
 * This is the same algorithm as used by (and copied from) `kotlin.random.XorWowRandom`.
 * TODO proper attribution following license terms, if applicable
 */
@Serializable
data class XorWowState(
    val x: Int,
    val y: Int,
    val z: Int,
    val w: Int,
    val v: Int,
    val addend: Int
) {
    
    companion object {
        fun fromSeed(seed: Long): XorWowState {
            val seed1 = seed.toInt()
            val seed2 = seed.shr(32).toInt()
            return XorWowState(
                x = seed1,
                y = seed2,
                z = 0,
                w = 0,
                v = seed1.inv(),
                addend = (seed1 shl 10) xor (seed2 ushr 4)
            )
                // some trivial seeds can produce several values with zeroes in upper bits, so we discard first 64
                .letRepeat(64) { it.next() }
        }
    }
    
    init {
        // TODO how to know if initial? or does that actually not matter and was this worded ambiguously?
//        require((x or y or z or w or v) != 0) { "Initial state must have at least one non-zero element." }
    }
    
    fun next(): XorWowState {
        // Equivalent to the xorxow algorithm
        // From Marsaglia, G. 2003. Xorshift RNGs. J. Statis. Soft. 8, 14, p. 5
        var t = x
        t = t xor (t ushr 2)
        val x = y
        val y = z
        val z = w
        val v0 = v
        val w = v0
        t = (t xor (t shl 1)) xor v0 xor (v0 shl 4)
        val v = t
        return XorWowState(x, y, z, w, v, addend + 362437)
    }
    
    fun int(): Int = v + addend
    fun bits(bitCount: Int): Int = int().takeUpperBits(bitCount)
    
    fun asRandom() = TransparentRandom(this)
    
    fun <T> next(n: (Random) -> T): Pair<T, XorWowState> =
        asRandom().let { Pair(n(it), it.state) }
    
    operator fun <T> invoke(n: (Random) -> T) = next(n)
    
}