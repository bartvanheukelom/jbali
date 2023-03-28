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
     *
     * If values are equal, returns the first one.
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

/**
 * Whether this [Iterable] is sorted according to the given natural [order].
 *
 * If the iterable is empty or has only one element, returns true. Consecutive elements
 * that compare equal are considered sorted.
 */
fun <T, K : Comparable<K>> Iterable<T>.isSortedInOrderBy(
    order: SortOrder = SortOrder.ASCENDING,
    selector: (T) -> K, // = { it } <- gives type mismatch so added overload below
): Boolean {
    val it = iterator()
    if (!it.hasNext()) return true
    var prev = selector(it.next())
    val comp = order.comparator<K>()
    while (it.hasNext()) {
        val next = selector(it.next())
        if (comp.compare(prev, next) > 0) return false
        prev = next
    }
    return true
}
fun <T : Comparable<T>> Iterable<T>.isSortedInOrderBy(
    order: SortOrder = SortOrder.ASCENDING,
): Boolean = isSortedInOrderBy(order) { it }
