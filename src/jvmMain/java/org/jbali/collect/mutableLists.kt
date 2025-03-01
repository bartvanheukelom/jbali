package org.jbali.collect

/**
 * Remove the head of entries matching the predicate from this mutable list.
 * That is, removes the first element until the predicate returns false.
 */
fun <T> MutableList<T>.removeWhile(predicate: (T) -> Boolean) {
    val li = listIterator()
    while (li.hasNext()) {
        if (!predicate(li.next())) {
            break
        }
        li.remove()
    }
}

/**
 * Remove the tail of entries matching the predicate from this mutable list.
 * That is, removes the last element until the predicate returns false.
 */
fun <T> MutableList<T>.removeLastWhile(predicate: (T) -> Boolean) {
    val li = listIterator(size)
    while (li.hasPrevious()) {
        if (!predicate(li.previous())) {
            break
        }
        li.remove()
    }
}

/**
 * Remove all entries matching the predicate from this mutable list,
 * even if they are interleaved with entries that don't.
 */
fun <T> MutableList<T>.removeAll(predicate: (T) -> Boolean) {
    removeIf(predicate)
}

/**
 * Remove the first entry matching the predicate from this mutable list.
 * Returns true if an entry was removed, false otherwise.
 */
fun <T> MutableList<T>.removeFirst(predicate: (T) -> Boolean): Boolean {
    val li = listIterator()
    while (li.hasNext()) {
        if (predicate(li.next())) {
            li.remove()
            return true
        }
    }
    return false
}

/**
 * Remove the last entry matching the predicate from this mutable list.
 * Returns true if an entry was removed, false otherwise.
 */
fun <T> MutableList<T>.removeLast(predicate: (T) -> Boolean): Boolean {
    val li = listIterator(size)
    while (li.hasPrevious()) {
        if (predicate(li.previous())) {
            li.remove()
            return true
        }
    }
    return false
}
