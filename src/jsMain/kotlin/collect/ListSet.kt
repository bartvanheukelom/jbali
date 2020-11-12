package org.jbali.collect

/**
 * Marker interface that indicates both [List] and [Set] semantics.
 */
expect interface ListSet<out T> : List<T>, Set<T>, Collection<T>, Iterable<T> {

}
