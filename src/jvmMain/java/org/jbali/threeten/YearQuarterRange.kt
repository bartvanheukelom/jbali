package org.jbali.threeten

import org.threeten.extra.YearQuarter
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Represents a range of [YearQuarter]s starting at [from] (inclusive) and ending at [to] (exclusive).
 * Can also be treated as a [org.jbali.collect.ListSet]<[YearQuarter]>, containing each quarter in the range.
 *
 * The range can be empty, but not negative/backwards, i.e. [from] cannot be later than [to].
 */
class YearQuarterRange(
        from: YearQuarter,
        to: YearQuarter
) : LocalDateUnitRange<YearQuarter>(from, to) {

    override val chronoUnit get() =
        ChronoUnit.MONTHS
    override val chronoUnitDivision: Int
        get() = 3

    override fun YearQuarter.firstDay(): LocalDate =
            atDay(1)

    override fun YearQuarter.plus(i: Long): YearQuarter =
            plusQuarters(i)

    override fun newSelf(from: YearQuarter, to: YearQuarter) =
            YearQuarterRange(from, to)

}