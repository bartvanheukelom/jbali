package org.jbali.streams

import java.util.stream.Stream
import java.util.stream.StreamSupport

fun <T> Iterable<T>.stream(): Stream<T> = StreamSupport.stream(spliterator(), false)

fun <C, V> Stream<V>.collect(supplier: () -> C, adder: C.(inValue: V) -> Unit): C {
    return collectTo(supplier(), adder)
}

fun <C, V> Stream<V>.collectTo(destination: C, adder: C.(inValue: V) -> Unit): C {
    forEach { destination.adder(it) }
    return destination
}

fun <C, V> Stream<V>.simpleReduce(base: C, combiner: (base: C, addValue: V) -> C): C {
    var b = base
    forEach { b = combiner(b, it) }
    return b
}
