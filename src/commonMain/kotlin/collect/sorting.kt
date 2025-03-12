package org.jbali.collect

/**
 * A copy of the list where _consecutive_ duplicates are merged into one.
 * Faster than [distinct] if the list is already sorted.
 *
 * @param equal determines if two elements are equal
 */
fun <T> List<T>.distinctFastWith(equal: (T, T) -> Boolean): List<T> {
    val src = this
    if (src.isEmpty()) return src
    var last = src[0]
    val dst = mutableListOf(last)
    for (i in src.subList(1, src.size)) {
        if (!equal(last, i)) {
            dst.add(i)
            last = i
        }
    }
    return dst
}
