package org.jbali.collect

import java.util.*
import kotlin.collections.ArrayList

/**
 * Marker interface that indicates both [List] and [Set] semantics.
 */
interface ListSet<T> : List<T>, Set<T>, Collection<T>, Iterable<T> {

    fun filter(predicate: (T) -> Boolean): ListSet<T> {
        return ListSetImpl(uniqueItemList = filterTo(ArrayList(), predicate))
    }

    override fun spliterator(): Spliterator<T> =
            Spliterators.spliterator(this, Spliterator.ORDERED)
}

/**
 * [ListSet] implementation that delegates all behaviour to a [List]
 * that has been verified to contain only unique items.
 */
class ListSetImpl<T>

/**
 * Construct around a list that is already known to contain only unique items.
 */
internal constructor(
        private val uniqueItemList: List<T>
) : ListSet<T>, List<T> by uniqueItemList, RandomAccess {

    override val size = uniqueItemList.size

    override fun spliterator(): Spliterator<T> =
            uniqueItemList.spliterator()

    override fun toString() =
            uniqueItemList.toString()

    constructor(items: Iterable<T>) : this(
            uniqueItemList = items
                    .ensureRandomAccessImmutableList()
                    .checkUnique()
    )
}

/**
 * @return This iterable if it implements [ListSet], or a copy of it.
 * @throws IllegalArgumentException if the iterable contains duplicate entries, i.e. does not adhere to [Set] semantics.
 */
fun <T> Iterable<T>.toListSet() =
        if (this is ListSet) {
            this
        } else {
            ListSetImpl(this)
        }
