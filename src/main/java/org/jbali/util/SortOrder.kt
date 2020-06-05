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
}
