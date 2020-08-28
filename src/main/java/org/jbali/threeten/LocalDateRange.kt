package org.jbali.threeten

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Represents a range of [LocalDate]s starting at [from] (inclusive) and ending at [to] (exclusive).
 * Can also be treated as a [List]<[LocalDate]>, containing each day in the range.
 *
 * The range can be empty, but not negative/backwards, i.e. [from] cannot be later than [to].
 */
class LocalDateRange(
        val from: LocalDate,
        val to: LocalDate
) : List<LocalDate> {

    init {
        require(from <= to) {
            "$from > $to"
        }
    }

    override fun toString(): String =
            "$from until $to"

    override val size: Int
        get() = ChronoUnit.DAYS.between(from, to).toInt()

    override fun contains(element: LocalDate): Boolean =
            element >= from && element < to

    override fun containsAll(elements: Collection<LocalDate>): Boolean =
            when (elements) {
                is LocalDateRange -> elements.from >= from && elements.to <= to
                else -> elements.all { it in this }
            }

    override fun equals(other: Any?): Boolean =
            when (other) {
                is LocalDateRange ->
                    from == other.from && to == other.to
                null ->
                    false
                is List<*> ->
                    size == other.size && toList() == other
                else ->
                    toList() == other
            }

    override fun hashCode(): Int =
            from.hashCode() + (31 * to.hashCode())

    override fun get(index: Int): LocalDate =
            if (index < size) {
                from.plusDays(index.toLong())
            } else {
                throw IndexOutOfBoundsException()
            }

    override fun indexOf(element: LocalDate): Int =
            if (element in this) {
                ChronoUnit.DAYS.between(from, element).toInt()
            } else {
                -1
            }

    override fun isEmpty(): Boolean =
            from == to

    override fun lastIndexOf(element: LocalDate): Int =
            indexOf(element)

    override fun iterator(): Iterator<LocalDate> =
            listIterator()

    override fun listIterator(): ListIterator<LocalDate> =
            listIterator(0)

    override fun listIterator(index: Int): ListIterator<LocalDate> =
            object : ListIterator<LocalDate> {

                var nxt: LocalDate = get(index)

                override fun hasNext(): Boolean =
                        nxt < to

                override fun next(): LocalDate =
                        if (hasNext()) {
                            val cur = nxt
                            nxt = nxt.plusDays(1)
                            cur
                        } else {
                            throw NoSuchElementException()
                        }

                override fun hasPrevious(): Boolean =
                        nxt >= from

                override fun previous(): LocalDate =
                        if (hasPrevious()) {
                            nxt = nxt.minusDays(1)
                            nxt
                        } else {
                            throw NoSuchElementException()
                        }

                override fun nextIndex(): Int =
                        indexOf(nxt)

                override fun previousIndex(): Int =
                        indexOf(nxt) - 1

            }

    override fun subList(fromIndex: Int, toIndex: Int): List<LocalDate> =
            LocalDateRange(this[fromIndex], this[toIndex])


}