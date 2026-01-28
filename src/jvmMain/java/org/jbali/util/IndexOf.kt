package org.jbali.util

import org.jbali.math.divmod

/**
 * Represents an index and its corresponding total in a sequence or collection.
 *
 * @property index The current index value.
 * @property total The total number of items.
 *
 * The `IndexOf` class ensures that the `index` is non-negative and within the range of the `total`.
 * If these conditions are not met, an exception is thrown upon initialization.
 */
data class IndexOf(
        val index: Int,
        val total: Int
) {
    init {
        require(total >= 0 && index >= 0 && index < total) {
            "Invalid $this"
        }
    }

    override fun toString() =
            "$index/$total"

    /**
     * Rotates the current index by the specified shift value within the bounds of the total.
     * The resulting index is calculated using Euclidean division to ensure it remains valid in the range `[0, total)`.
     * For example, `IndexOf(2, 5).rotate(3) == IndexOf(0, 5)`.
     *
     * @param shift The number of positions by which to shift the index. Can be positive (shift forward) or negative (shift backward).
     * @return A new `IndexOf` instance with the rotated index and the same total.
     */
    fun rotate(shift: Int) =
        IndexOf(index.plus(shift).divmod(total), total)

    companion object {
        /**
         * Generates a sequence of `IndexOf` instances for indices ranging from 0 to the specified total (exclusive).
         *
         * @param total The total number of elements in the sequence. Must be non-negative.
         * @return A sequence of `IndexOf` objects, each representing an index and the total.
         * @throws IllegalArgumentException If the total is negative.
         */
        fun range(total: Int): Sequence<IndexOf> =
                (0 until total).asSequence().map {
                    IndexOf(it, total)
                }
    }
}


/**
 * Creates an `IndexOf` instance using the current integer as the index and the provided total.
 *
 * @param total The total number of elements in the collection. Must be non-negative and greater than the current integer.
 * @return An `IndexOf` object representing the current integer as the index and the specified total.
 * @throws IllegalArgumentException If the current integer (as the index) is negative, exceeds the total, or if the total is negative.
 */
infix fun Int.of(total: Int) = IndexOf(this, total)
