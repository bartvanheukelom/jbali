package org.jbali.threeten

import java.time.Duration
import java.time.Instant
import java.util.*

// TODO instantMinDate etc

/**
 * The latest [Instant] that can accurately be represented by [Date].
 */
val instantMaxDate: Instant =
        Instant.ofEpochMilli(Long.MAX_VALUE)

/**
 * The latest [Instant] that can be converted to [Date] without [ArithmeticException] being thrown,
 * but while losing its nanosecond precision.
 */
val instantVeryMaxDate: Instant =
        Instant.ofEpochMilli(Long.MAX_VALUE).plusNanos(999_999L)

// TODO toDateExact
fun Instant.toDate(): Date =
        try {
            Date(this.toEpochMilli())
        } catch (e: ArithmeticException) {
            throw ArithmeticException("$this cannot be represented as Date due to long overflow")
        }

/**
 * The length of this duration in seconds, as double, with millisecond precision
 * (if available from double).
 */
val Duration.secondsDouble get() =
        toMillis().toDouble() / 1000.0

/**
 * The length of this duration in days, as double, with millisecond precision
 * (if available from double).
 */
val Duration.daysDouble get() =
        toMillis().toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)


fun nanos(x: Long): Duration = Duration.ofNanos(x)
fun millis(x: Long): Duration = Duration.ofMillis(x)
fun seconds(x: Long): Duration = Duration.ofSeconds(x)
fun minutes(x: Long): Duration = Duration.ofMinutes(x)
fun hours(x: Long): Duration = Duration.ofHours(x)
fun days(x: Long): Duration = Duration.ofDays(x)


fun duration(
        nanos: Long = 0,
        millis: Long = 0,
        seconds: Long = 0,
        minutes: Long = 0,
        hours: Long = 0,
        days: Long = 0
) =
        (
                Duration.ofNanos(nanos) +
                Duration.ofMillis(millis) +
                Duration.ofSeconds(seconds)+
                Duration.ofMinutes(minutes)+
                Duration.ofHours(hours)+
                Duration.ofDays(days)
        )!!
