package org.jbali.threeten

import org.threeten.extra.YearWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Represents a range of [YearWeek]s starting at [from] (inclusive) and ending at [to] (exclusive).
 * Can also be treated as a [org.jbali.collect.ListSet]<[YearWeek]>, containing each week in the range.
 *
 * The range can be empty, but not negative/backwards, i.e. [from] cannot be later than [to].
 */
class YearWeekRange(
        from: YearWeek,
        to: YearWeek
) : LocalDateUnitRange<YearWeek>(from, to) {

    override val chronoUnit get() =
        ChronoUnit.WEEKS

    override fun YearWeek.firstDay(): LocalDate =
            atDay(DayOfWeek.MONDAY)

    override fun YearWeek.plus(i: Long): YearWeek =
            plusWeeks(i)

    override fun newSelf(from: YearWeek, to: YearWeek) =
            YearWeekRange(from, to)

}