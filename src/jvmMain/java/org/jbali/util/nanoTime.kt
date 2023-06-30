@file:OptIn(ExperimentalUnsignedTypes::class)

package org.jbali.util

import org.jbali.math.toUIntClamped
import java.time.Duration


// --- NanoTime --- //

/**
 * Represents a monotonic nanosecond time as returned by [System.nanoTime].
 */
@JvmInline value class NanoTime(val nt: Long) : Comparable<NanoTime> {
    fun toMicro() = MicroTime(nt / 1_000L)
    override fun toString() = "nT$nt"
    override fun compareTo(other: NanoTime) = nt.compareTo(other.nt)
    companion object {
        fun now() = NanoTime(System.nanoTime())
    }
}

/**
 * Represents a length of time in nanoseconds, or more specifically, a time difference,
 * as it may be negative.
 */
@JvmInline value class NanoDuration(val ns: Long) : Comparable<NanoDuration> {
    override fun toString() = "$ns ns"
    override fun compareTo(other: NanoDuration) = ns.compareTo(other.ns)
    operator fun plus(other: NanoDuration) = NanoDuration(ns + other.ns)
    operator fun minus(other: NanoDuration) = NanoDuration(ns - other.ns)
    companion object {
        val MAX = NanoDuration(Long.MAX_VALUE)
        fun since(from: NanoTime) = between(from, NanoTime.now())
        fun between(from: NanoTime, to: NanoTime) = NanoDuration(to.nt - from.nt)
    }
}

fun Duration.toNanoDuration() = NanoDuration(this.toNanos())


// --- MicroTime --- //

/**
 * Represents a monotonic microsecond time as returned by [System.nanoTime] / 1'000.
 * @see NanoTime
 */
@JvmInline value class MicroTime(val ut: Long) {
    fun toNano() = NanoTime(ut * 1_000L)
    override fun toString() = "μT$ut"
    operator fun plus(other: MicroDuration) = MicroTime(ut + other.us)
    operator fun minus(other: MicroDuration) = MicroTime(ut - other.us)
    companion object {
        fun now() = NanoTime.now().toMicro()
    
        /**
         * The difference between the given times as a [UInt], clamped as in [Long.toUIntClamped],
         * meaning it can only accurately represent >= 0 differences up to ~4'295 seconds.
         */
        fun diffUIntClamped(from: MicroTime, to: MicroTime) =
            MicroDuration.between(from, to).us.toUIntClamped()
    }
}

/**
 * Represents a length of time in microseconds, or more specifically, a time difference,
 * as it may be negative.
 */
@JvmInline value class MicroDuration(val us: Long) {
    override fun toString() = "$us μs"
    companion object {
        fun since(from: MicroTime) = between(from, MicroTime.now())
        fun between(from: MicroTime, to: MicroTime) = MicroDuration(to.ut - from.ut)
    }
}

infix fun MicroTime.diffUIntClampedTo(to: MicroTime) = MicroTime.diffUIntClamped(this, to)
