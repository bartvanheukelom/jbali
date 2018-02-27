package org.jbali.errors

import java.lang.Integer.min
import java.util.*
import kotlin.NoSuchElementException

/**
 * Remove the bottom part of e's stack that matches with
 * the current call stack.
 *
 * E.g. if e's stack is
 * - internalCode()
 * - someMethod()
 * - handler()
 * - requestReceiver()
 * - readFromSocket()
 * - Thread.run()
 *
 * and you're calling removeCommonStack(e) from handler(), e's stack will be truncated to
 * - internalCode()
 * - someMethod()
 * - handler()
 */
fun Throwable.removeCurrentStack() {

    val locTrace = Thread.currentThread().stackTrace
    // you never know on some VMs
    if (locTrace.size < 4) return

    // 0 = Thread.getStackTrace
    // 1 = removeCommonStack
    val thisMethod = locTrace[2]
    val caller = locTrace[3]

    removeStackFrom(thisMethod, caller)

}

// TODO refactor to variable needle length
fun Throwable.removeStackFrom(thisMethod: StackTraceElement, caller: StackTraceElement) {

    for (toClean in this.causeChain) {

        val errTrace = toClean.stackTrace

        // find the calling method in the exception stack
        if (errTrace.size >= 3) for (i in errTrace.size - 1 downTo 1) {
            if (errTrace[i] == caller) {
                // check if the calling method did in fact call this method
                // (line number won't match)
                val nextCall = errTrace[i - 1]
                if (nextCall.className == thisMethod.className && nextCall.methodName == thisMethod.methodName) {
                    // snip!
                    toClean.stackTrace = Arrays.copyOfRange(errTrace, 0, i)
                    break
                }
            }
        }
    }
}

fun Throwable.removeCommonStack() {
    val locTrace = Thread.currentThread().stackTrace.toList()
    for (toClean in this.causeChain) {
        val errTrace = toClean.stackTrace
        toClean.stackTrace = Arrays.copyOfRange(errTrace, 0, errTrace.size-commonTailLength(locTrace, errTrace.toList()))
    }
}

fun <T> commonHead(a: List<T>, b: List<T>): List<T> {
    var i = 0
    while (i < min(a.size, b.size) && a[i] == b[i]) i++
    return a.subList(0, i)
}

fun <T> commonTail(a: List<T>, b: List<T>) = a.subList(a.size- commonTailLength(a, b), a.size)

fun commonTailLength(a: List<*>, b: List<*>): Int {
    var i = 0
    while (i < min(a.size, b.size) && a[a.size-i-1] == b[b.size-i-1]) i++
    return i
}

val Throwable.causeChain: Iterable<Throwable> get() =
    independentIterable(this, Throwable::cause)

fun <T> independentIterable(start: T, next: (cur: T) -> T?) =
    object : Iterable<T> {
        override fun iterator() = independentIterator(start, next)
    }

fun <T> independentIterator(start: T, next: (cur: T) -> T?) =
    object : Iterator<T> {
        var nxt: T? = start
        override fun hasNext() = nxt != null
        override fun next(): T {
            if (nxt == null) throw NoSuchElementException()
            val r = nxt!!
            nxt = next(nxt!!) // so next-next :D
            return r
        }
    }
