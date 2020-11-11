package org.jbali.threeten

import org.jbali.collect.ListSet
import org.jbali.reflect.kClass
import java.time.LocalDate
import java.time.temporal.ChronoUnit

abstract class LocalDateUnitRange<U>(
        val from: U,
        val to: U
)
    : ListSet<U>
        where U : Comparable<U>
{
    init {
        require(from <= to) {
            "$from > $to"
        }
    }

    protected abstract val chronoUnit: ChronoUnit
    protected open val chronoUnitDivision: Int get() = 1
    protected abstract fun U.firstDay(): LocalDate
    protected abstract operator fun U.plus(i: Long): U
    protected abstract fun newSelf(from: U, to: U): LocalDateUnitRange<U>

    @Suppress("LeakingThis") private val fromFirst = from.firstDay()
    @Suppress("LeakingThis") private val   toFirst =   to.firstDay()

    override fun toString(): String =
            "$from until $to"

    override val size: Int
        get() = uncappedIndexOf(to)

    override operator fun contains(element: U): Boolean =
            element >= from && element < to

    open operator fun contains(element: LocalDate): Boolean =
            containsDate(element)

    // exists specifically so LocalDateRange can call it
    protected fun containsDate(date: LocalDate) =
            date >= fromFirst && date < toFirst

    override fun containsAll(elements: Collection<U>): Boolean =
            when (elements) {
                is LocalDateUnitRange -> elements.from >= from && elements.to <= to
                else -> elements.all { it in this }
            }

    override fun equals(other: Any?): Boolean =
            when {
                other == null ->
                    false
                other.kClass == kClass -> {
                    @Suppress("UNCHECKED_CAST")
                    other as LocalDateUnitRange<U>
                    from == other.from && to == other.to
                }
                other is List<*> ->
                    size == other.size && toList() == other
                else ->
                    toList() == other
            }

    override fun hashCode(): Int =
            from.hashCode() + (31 * to.hashCode())

    override fun get(index: Int): U =
            @Suppress("ConvertTwoComparisonsToRangeCheck")
            if (index >= 0 && index < size) {
                from + index.toLong()
            } else {
                throw IndexOutOfBoundsException()
            }

    override fun indexOf(element: U): Int =
            if (element in this) {
                uncappedIndexOf(element)
            } else {
                -1
            }

    private fun uncappedIndexOf(element: U) =
            chronoUnit.between(fromFirst, element.firstDay()).toInt() / chronoUnitDivision

    override fun isEmpty(): Boolean =
            from == to

    override fun lastIndexOf(element: U): Int =
            indexOf(element)

    override fun iterator(): Iterator<U> =
            listIterator()

    override fun listIterator(): ListIterator<U> =
            listIterator(0)

    override fun listIterator(index: Int): ListIterator<U> =
            object : ListIterator<U> {

                var nxt: U = get(index)

                override fun hasNext(): Boolean =
                        nxt < to

                override fun next(): U =
                        if (hasNext()) {
                            val cur = nxt
                            nxt += 1L
                            cur
                        } else {
                            throw NoSuchElementException()
                        }

                override fun hasPrevious(): Boolean =
                        nxt >= from

                override fun previous(): U =
                        if (hasPrevious()) {
                            nxt += -1L
                            nxt
                        } else {
                            throw NoSuchElementException()
                        }

                override fun nextIndex(): Int =
                        indexOf(nxt)

                override fun previousIndex(): Int =
                        indexOf(nxt) - 1

            }

    override fun subList(fromIndex: Int, toIndex: Int): List<U> =
            newSelf(this[fromIndex], this[toIndex])


}