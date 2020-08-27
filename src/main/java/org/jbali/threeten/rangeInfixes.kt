package org.jbali.threeten

import org.threeten.extra.Interval
import java.time.Instant
import java.time.LocalDate

/**
 * Returns a range from this value up to but excluding the specified [to] value.
 */
infix fun LocalDate.until(to: LocalDate) =
        LocalDateRange(this, to)

infix fun Instant.until(to: Instant): Interval =
        Interval.of(this, to)
