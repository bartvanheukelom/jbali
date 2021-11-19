package org.jbali.collect

fun <T, R> Iterator<T>.map(mapper: (T) -> R): Iterator<R> =
    object : Iterator<R> {
        override fun hasNext() = this@map.hasNext()
        override fun next(): R = mapper(this@map.next())
    }
