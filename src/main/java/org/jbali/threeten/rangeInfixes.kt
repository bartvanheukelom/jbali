package org.jbali.threeten

import org.threeten.extra.Interval
import java.time.Instant
import java.time.LocalDate

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 */
infix fun LocalDate.until(to: LocalDate) =
        LocalDateRange(this, to)

/**
 * Obtains an instance of [Interval] from this instant up to but excluding the specified [to] instant.
 * @receiver the start instant, inclusive, [Instant.MIN] treated as unbounded
 * @param to the end instant, exclusive, [Instant.MAX] treated as unbounded
 * @return the half-open interval
 * @throws java.time.DateTimeException if the end is before the start
 */
infix fun Instant.until(to: Instant): Interval =
        Interval.of(this, to)
