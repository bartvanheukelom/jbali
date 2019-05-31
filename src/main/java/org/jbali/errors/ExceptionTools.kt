package org.jbali.errors

import org.jbali.threads.withValue
import java.lang.Integer.min
import java.util.*
import kotlin.NoSuchElementException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
    // +1 = removeCurrentStack
    currentStackSignature(1)?.let { removeStackFrom(it) }
}

data class StackSignature(
        val thisMethod: StackTraceElement,
        val caller: StackTraceElement
)

// using an overload instead of an optional parameter, because the latter would generate
// a synthetic method $default that would add an extra stack frame
fun currentStackSignature(): StackSignature? = currentStackSignature(1)
fun currentStackSignature(extraOffset: Int): StackSignature? {
    val t = Thread.currentThread().stackTrace
    // 0 = Thread.getStackTrace
    // 1 = currentStackSignature
    return if (t.size <= 3 + extraOffset) null
    else StackSignature(t[2 + extraOffset], t[3 + extraOffset])
}

// TODO refactor to variable needle length
fun Throwable.removeStackFrom(sig: StackSignature) {

    for (toClean in this.causeChain) {

        val errTrace = toClean.stackTrace

        // find the calling method in the exception stack
        if (errTrace.size >= 3) for (i in errTrace.size - 1 downTo 1) {
            if (errTrace[i] == sig.caller) {
                // check if the calling method did in fact call this method
                // (line number won't match)
                val nextCall = errTrace[i - 1]
                if (nextCall.className  == sig.thisMethod.className &&
                    nextCall.methodName == sig.thisMethod.methodName) {
                        // snip!
                        toClean.stackTrace = Arrays.copyOfRange(errTrace, 0, i)
                    break
                }
            }
        }
    }
}

/**
 * Don't use directly, use stackRoot()
 */
val currentStackBase = ThreadLocal<StackSignature?>()
// TODO @InlineOnly
@UseExperimental(ExperimentalContracts::class)
inline fun <T> stackRoot(body: () -> T): T {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    return currentStackBase.withValue(currentStackSignature(), body)
}

// TODO use receiver context
fun Throwable.removeStackRoot() {
    currentStackBase.get()?.let { removeStackFrom(it) }
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

/**
 * Iterates this exception and all its causes
 */
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

fun ensureUnchecked(e: Throwable) =
        e as? RuntimeException ?: RuntimeException(e)
