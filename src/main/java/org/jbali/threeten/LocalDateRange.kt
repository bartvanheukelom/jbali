package org.jbali.threeten

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Represents a range of [LocalDate]s starting at [from] (inclusive) and ending at [to] (exclusive).
 * Can also be treated as a [org.jbali.collect.ListSet]<[LocalDate]>, containing each day in the range.
 *
 * The range can be empty, but not negative/backwards, i.e. [from] cannot be later than [to].
 */
class LocalDateRange(
        from: LocalDate,
        to: LocalDate
) : LocalDateUnitRange<LocalDate>(from, to) {

    override val chronoUnit get() =
        ChronoUnit.DAYS

    override fun LocalDate.firstDay(): LocalDate =
            this

    override fun LocalDate.plus(i: Long): LocalDate =
            plusDays(i)

    override fun newSelf(from: LocalDate, to: LocalDate) =
            LocalDateRange(from, to)

    // disambiguating override
    override fun contains(element: LocalDate) =
        containsDate(element)

}