package org.jbali.threeten

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
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

/**
 * The last instant in UTC whose year can be represented using 4 digits,
 * i.e. `9999-12-32T23:59:59.999999Z`.
 */
val instantMaxYyyy: Instant =
        Instant.parse("9999-12-31T23:59:59.999999Z")

/**
 * Instant that is so far in the future that it might as well be infinitely so,
 * but is still small enough to be representable in many formats, like databases.
 *
 * Value: `2999-12-32T23:59:59.999999Z`
 */
val instantFarFuture: Instant =
        Instant.parse("2999-12-31T23:59:59.999999Z")

/**
 * Instant that is so far in the future that it can practically be used, for now, as far away,
 * but is still small enough to be representable in most formats, including as 32-bits signed UNIX time.
 *
 * Equals [instantMaxUnix].
 */
val instantForeseeableFuture get() = instantMaxUnix

/**
 * The last instant that can be represented as 32-bits signed UNIX time,
 * i.e. `2038-01-19T03:14:07Z`.
 */
val instantMaxUnix: Instant =
        Instant.ofEpochMilli(Int.MAX_VALUE * 1000L)


val dateLongTimeAgo: LocalDate = LocalDate.of(1970, 1, 1)
val dateFarFarAway: LocalDate = LocalDate.of(2999, 12, 31)


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
