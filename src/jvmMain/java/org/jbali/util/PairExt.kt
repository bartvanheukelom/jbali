package org.jbali.util

val Pair<*, *>.assignFormatted get() = "${first}=${second}"

typealias Unipair<T> = Pair<T, T>

/**
 * Returns a pair containing the results of applying the given [transform] function
 * to each element in the original pair].
 */
fun <T, R> Unipair<T>.map(transform: (T) -> R): Unipair<R> =
    Pair(transform(first), transform(second))
