package org.jbali.threeten

import java.time.LocalDate
import java.time.Year
import java.time.temporal.ChronoUnit

/**
 * Represents a range of [Year]s starting at [from] (inclusive) and ending at [to] (exclusive).
 * Can also be treated as a [org.jbali.collect.ListSet]<[Year]>, containing each quarter in the range.
 *
 * The range can be empty, but not negative/backwards, i.e. [from] cannot be later than [to].
 */
class YearRange(
        from: Year,
        to: Year
) : LocalDateUnitRange<Year>(from, to) {

    override val chronoUnit get() =
        ChronoUnit.YEARS

    override fun Year.firstDay(): LocalDate =
            atDay(1)

    override fun Year.plus(i: Long): Year =
            plusYears(i)

    override fun newSelf(from: Year, to: Year) =
            YearRange(from, to)

}
