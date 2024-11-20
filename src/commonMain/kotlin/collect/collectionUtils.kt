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


/**
 * Performs the given [action] on the sequential index, key and value of each element.
 *
 * An function with overloads for `Map<K, V>` and `List<Pair<K, V>>` can use this to provide the looping implementation
 * without converting between [Pair] and [Map.Entry] for each element.
 *
 * @param [key] function that extracts the key from an element.
 * @param [value] function that extracts the value from an element.
 */
inline fun <E : Any, K, V> Iterable<E>.forEachEntryIndexed(
        key: (E) -> K,
        value: (E) -> V,
        action: (Int, K, V) -> Unit
) {
    var i = 0
    forEach { e ->
        action(i++, key(e), value(e))
    }
}

/**
 * @return This iterable if it implements [RandomAccess], otherwise a [List] copy of it.
 */
fun <T> Iterable<T>.ensureRandomAccessImmutableList(): List<T> =
        if (this is List && this is RandomAccess) {
            this
        } else {
            this.toList()
        }

/**
 * Checks whether this iterable contains any duplicate items.
 * @return This iterable if it doesn't contain any duplicates.
 * @throws IllegalArgumentException if duplicates have been found, with a message describing exactly which.
 */
fun <T : Iterable<*>> T.checkUnique(): T {
    val dups = groupBy { it }
            .mapValues { it.value.size }
            .filter { it.value > 1 }
    if (dups.isNotEmpty()) {
        throw IllegalArgumentException("Duplicate entries: $dups")
    } else {
        return this
    }
}

/**
 * @return the single element in this iterable, or `null` if it's empty.
 * @throws IllegalArgumentException if there is more than one element.
 */
fun <T> List<T>.singleIfAny(): T? =
    when (size) {
        0 -> null
        1 -> this[0]
        else -> throw IllegalArgumentException("List has more than one element.")
    }

/**
 * @return the single element in this iterable, or `null` if it's empty.
 * @throws IllegalArgumentException if there is more than one element.
 */
fun <T> Iterable<T>.singleIfAny(): T? =
    when (this) {
        is List -> singleIfAny()
        else -> {
            val iterator = iterator()
            if (!iterator.hasNext()) {
                null
            } else {
                val single = iterator.next()
                if (iterator.hasNext()) {
                    throw IllegalArgumentException("Collection has more than one element.")
                } else {
                    single
                }
            }
        }
    }
