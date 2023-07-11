@file:OptIn(ExperimentalContracts::class)

package org.jbali.random

import kotlin.contracts.ExperimentalContracts
import kotlin.random.Random


/**
 * Random number generator that exposes its internal [state].
 * This state can be logged, e.g. for debugging or auditing purposes.
 * It can also be used to fully restore / reproduce a [TransparentRandom] instance that
 * will produce exactly the same sequence of numbers as the original instance would have.
 *
 * Implemented using Marsaglia's "xorwow" algorithm, see [XorWowState].
 * This is the same algorithm as used by `kotlin.random.XorWowRandom`,
 * and the following two RNG's would produce the same sequence of numbers:
 *
 * ```
 * val seed: Long = 123456789L
 * val kr = kotlin.random.Random(seed)
 * val tr = TransparentRandom.fromSeed(seed)
 * ```
 */
class TransparentRandom(
    var state: XorWowState,
) : Random() {
    companion object {
        fun fromSeed(seed: Long) = TransparentRandom(XorWowState.fromSeed(seed))
    }
    
    override fun nextBits(bitCount: Int): Int {
        state = state.next()
        return state.bits(bitCount)
    }
}


fun Int.takeUpperBits(bitCount: Int): Int =
    this.ushr(32 - bitCount) and (-bitCount).shr(31)
