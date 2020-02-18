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
