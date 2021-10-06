@file:OptIn(ExperimentalUnsignedTypes::class)

package org.jbali.util

import org.jbali.math.toUIntClamped

/**
 * Represents a monotonic nanosecond time as returned by [System.nanoTime].
 */
@JvmInline value class NanoTime(val nt: Long) {
    fun toMicro() = MicroTime(nt / 1_000L)
    companion object {
        fun now() = NanoTime(System.nanoTime())
    }
}

/**
 * [NanoTime] but in µs.
 */
@JvmInline value class MicroTime(val ut: Long) {
    companion object {
        fun now() = NanoTime.now().toMicro()
    
        /**
         * The difference between the given times as a [UInt], clamped as in [Long.toUIntClamped],
         * meaning it can only accurately represent >= 0 differences up to ~4'295 seconds.
         */
        fun diffUIntClamped(from: MicroTime, to: MicroTime) =
            (to.ut - from.ut).toUIntClamped()
    }
}

infix fun MicroTime.diffUIntClampedTo(to: MicroTime) = MicroTime.diffUIntClamped(this, to)
