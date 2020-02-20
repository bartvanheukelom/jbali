package org.jbali.collect

/**
 * Returns a sequence of pairs as long as the longest of the given iterables,
 * each component of the pair containing the entry on that index of the respective iterable,
 * or `null` if that iterable is shorter than the other.
 *
 * Example: `longZip(listOf(1, 2), listOf("A")) -> [1 to "A", 2 to null]`
 */
// TODO inline forEach version
infix fun <A, B> Iterable<A>.longZip(b: Iterable<B>): Sequence<Pair<A?, B?>> =
        sequence {
            val a = this@longZip
            val ia = a.iterator()
            val ib = b.iterator()
            while (ia.hasNext() || ib.hasNext()) {
                val va = if (ia.hasNext()) ia.next() else null
                val vb = if (ib.hasNext()) ib.next() else null
                yield(va to vb)
            }
        }

/**
 * Returns the index of the last element matching the given [predicate], or -1 if no such element was found.
 */
inline fun <T> List<T>.findLastIndex(predicate: (T) -> Boolean): Int {
    val iterator = this.listIterator(size)
    while (iterator.hasPrevious()) {
        val index = iterator.previousIndex()
        val element = iterator.previous()
        if (predicate(element)) return index
    }
    return -1
}
