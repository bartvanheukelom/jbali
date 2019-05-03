package org.jbali.collect

/**
 * Use to procedurally split a source list into various slices of irregular sizes.
 * Each slice is backed by the source list, which should therefore be immutable.
 */
class ListSplitter<T>(val source: List<T>) {

    val split = source.spliterator()

    var takenFront = 0
    val sizeLeft get() = source.size - takenFront

    fun takeHead(n: Int): List<T> {
        require(n <= sizeLeft)
        takenFront += n
        return source.subList(takenFront - n, takenFront)
    }

    fun takeRest(): List<T> =
        takeHead(sizeLeft)
}

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
