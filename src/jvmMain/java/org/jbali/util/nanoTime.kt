package org.jbali.util

import org.jbali.math.toUIntClamped
import java.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.nanoseconds


// --- NanoTime --- //

/**
 * Represents a monotonic nanosecond time as returned by [System.nanoTime].
 */
@JvmInline value class NanoTime(val nt: Long) : Comparable<NanoTime> {
    
    override fun toString() = "nT$nt"
    
    fun toMicro() = MicroTime(nt / 1_000L)
    
    override fun compareTo(other: NanoTime) = nt.compareTo(other.nt)
    
    /**
     * @return the [NanoDuration] between this and now.
     */
    fun elapsed() = NanoDuration.since(this)
    
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
    
    /**
     * Convert this duration to a [MicroDuration], discarding any fractional microseconds.
     */
    fun toMicro() = MicroDuration(ns / 1_000L)
    
    /**
     * Convert this duration to seconds, as a [Double], which may be imprecise for large values.
     */
    fun toSeconds(): Double = ns / 1_000_000_000.0
    
    /**
     * Convert this duration to a [kotlin.time.Duration].
     */
    fun toKDuration() = ns.nanoseconds
    
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
    
    override fun toString() = "μT$ut"
    
    // toNano, not toNanos: short for toNanoTime
    fun toNano() = NanoTime(ut * 1_000L)
    
    operator fun plus(other: MicroDuration) = MicroTime(ut + other.us)
    operator fun minus(other: MicroDuration) = MicroTime(ut - other.us)
    
    companion object {
        fun now() = NanoTime.now().toMicro()
    
        /**
         * The microsecond difference between the given times as a 32-bits [UInt], clamped as in [Long.toUIntClamped],
         * meaning it can only accurately represent differences up to ~4'295 seconds (1+ hour), where [from] <= [to].
         */
        fun diffUIntClamped(from: MicroTime, to: MicroTime) =
            MicroDuration.between(from, to).us.toUIntClamped()
    }
}


/**
 * Represents a length of time in microseconds, or more specifically, a time difference,
 * as it may be negative.
 */
@JvmInline value class MicroDuration(val us: Long) : Comparable<MicroDuration> {
    
    override fun toString() = "$us μs"
    
    /**
     * Convert this duration to a [NanoDuration]. This can overflow if the duration is too large.
     */
    fun toNano() = NanoDuration(us * 1_000L)
    
    /**
     * Convert this duration to seconds, as a [Double], which may be imprecise for large values.
     */
    fun toSeconds() = us / 1_000_000.0
    
    /**
     * Convert this duration to a [kotlin.time.Duration].
     */
    fun toKDuration() = us.microseconds
    
    override fun compareTo(other: MicroDuration) = us.compareTo(other.us)
    
    operator fun plus(other: MicroDuration) = MicroDuration(us + other.us)
    operator fun minus(other: MicroDuration) = MicroDuration(us - other.us)
    
    companion object {
        fun since(from: MicroTime) = between(from, MicroTime.now())
        fun between(from: MicroTime, to: MicroTime) = MicroDuration(to.ut - from.ut)
    }
}

infix fun MicroTime.diffUIntClampedTo(to: MicroTime) = MicroTime.diffUIntClamped(this, to)
