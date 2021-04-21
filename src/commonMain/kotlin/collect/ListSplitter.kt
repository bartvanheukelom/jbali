@file:OptIn(ExperimentalJsExport::class)

package org.jbali.collect

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Use to procedurally split a source list into various slices of irregular sizes.
 * Each slice is backed by the source list, which should therefore be immutable.
 */
@JsExport
class ListSplitter<T>(val source: List<T>) {

    private var front = 0
    private var end = source.size
    val sizeLeft get() = end - front

    @JsName("takeHead")
    fun takeHead(n: Int): List<T> {
        require(n <= sizeLeft)
        front += n
        return source.subList(front - n, front)
    }

    @JsName("takeTail")
    fun takeTail(n: Int): List<T> {
        require(n <= sizeLeft)
        end -= n
        return source.subList(end, end + n)
    }

    fun takeRest(): List<T> =
        takeHead(sizeLeft)
}
