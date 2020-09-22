package org.jbali.threeten

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Represents a range of [YearMonth]s starting at [from] (inclusive) and ending at [to] (exclusive).
 * Can also be treated as a [org.jbali.collect.ListSet]<[YearMonth]>, containing each month in the range.
 *
 * The range can be empty, but not negative/backwards, i.e. [from] cannot be later than [to].
 */
class YearMonthRange(
        from: YearMonth,
        to: YearMonth
) : LocalDateUnitRange<YearMonth>(from, to) {

    override val chronoUnit get() =
        ChronoUnit.MONTHS

    override fun YearMonth.firstDay(): LocalDate =
            atDay(1)

    override fun YearMonth.plus(i: Long): YearMonth =
            plusMonths(i)

    override fun newSelf(from: YearMonth, to: YearMonth) =
            YearMonthRange(from, to)

}