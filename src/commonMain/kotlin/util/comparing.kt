package org.jbali.util

/**
 * Self-documenting constants for return values of [Comparator.compare].
 */
object CompareResult {
    const val Equal = 0
    const val ALess = -1
    const val AGreater = 1
    const val BGreater = -1
    const val BLess = 1
}
/**
 * Self-documenting constants for return values of [Comparable.compareTo].
 */
object CompareToResult {
    const val Equal = 0
    const val ThisLess = -1
    const val ThisGreater = 1
    const val OtherLess = -1
    const val OtherGreater = 1
}

/**
 * Compares this object with the specified object for order, with support for null.
 * - If neither is null, delegates to `T.compareTo(other: T)`.
 * - A single null compares less than non-null.
 * - 2 nulls are, of course, equal.
 */
@Deprecated("Use compareValues instead", ReplaceWith("compareValues(this, other)"))
fun <T : Comparable<T>> T?.compareNullable(other: T?): Int =
    when {
        this == null && other == null -> CompareToResult.Equal
        this == null                  -> CompareToResult.ThisLess
                        other == null -> CompareToResult.OtherLess
        else                          -> this.compareTo(other)
    }
