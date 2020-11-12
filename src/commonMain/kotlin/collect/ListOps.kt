package org.jbali.collect

fun <T> List<T>.subListSized(offset: Int, size: Int) =
        this.subList(offset, offset + size)

// TODO a new List class specialized in the following operations:

/**
 * Remove the first n items from the list and return them in the original order.
 */
fun <T> MutableList<T>.removeHead(n: Int): List<T> {
    val head = subList(0, n)
    val copy = head.toList()
    head.clear()
    return copy
}

/**
 * Returns a new List with the items from this list,
 * and clears this list.
 */
fun <T> MutableList<T>.moveAndClear(): List<T> {
    val copy = toList()
    clear()
    return copy
}
