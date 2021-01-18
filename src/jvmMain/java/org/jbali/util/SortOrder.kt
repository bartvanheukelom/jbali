package org.jbali.util

enum class SortOrder(
        /** 1 for ASCENDING, -1 for DESCENDING */
        val multiplier: Int
) {

    ASCENDING(1),
    DESCENDING(-1);

    fun <T : Comparable<T>> comparator(): Comparator<T> =
            when (this) {
                ASCENDING -> naturalOrder()
                DESCENDING -> reverseOrder()
            }

    /**
     * Given two values, returns the one that would be listed first in a hypothetical
     * list of [T] naturally sorted using this [SortOrder].
     */
    fun <T : Comparable<T>> firstOf(a: T, b: T) =
            when (this) {
                ASCENDING -> minOf(a, b)
                DESCENDING -> maxOf(a, b)
            }
}

/**
 * Depending on the value of [order], is equivalent to the stdlib functions:
 * - [SortOrder.ASCENDING] -> [Iterable.sortedBy]
 * - [SortOrder.DESCENDING] -> [Iterable.sortedByDescending]
 */
fun <T, K : Comparable<K>> Iterable<T>.sortedInOrderBy(order: SortOrder, selector: (T) -> K): List<T> =
    sortedWith(compareBy(order.comparator(), selector))
