@file:OptIn(ExperimentalContracts::class)

package org.jbali.errors

import org.jbali.collect.findLastIndex
import org.jbali.threads.withValue
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Integer.min
import java.util.*
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
 * and you're calling removeCurrentStack(e) from handler(), e's stack will be truncated to
 * - internalCode()
 * - someMethod()
 * - handler()
 *
 * @return Undo lambda, see [removeStackFrom].
 */
fun Throwable.removeCurrentStack(): () -> Unit =
    // +1 = removeCurrentStack
    currentStackSignature(1)
            ?.let { removeStackFrom(it) }
            ?: {}

/**
 * Run [block] while this exception's stacktrace is shortened
 * according to the rules of [removeCurrentStack].
 * After this method returns, the stack trace is as it was before this call.
 */
fun <T, E : Throwable> E.withoutCurrentStack(block: (E) -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    
    // +1 = withoutCurrentStack
    val sig = currentStackSignature(1)
    if (sig == null) {
        // no common stack so nothing to modify
        return block(this)
    } else {
        val undo = removeStackFrom(sig)
        try {
            return block(this)
        } finally {
            try {
                undo()
            } catch (e: Throwable) {
                addSuppressed(e)
            }
        }
    }
}

data class StackSignature(
        val callee: StackTraceElement,
        val caller: StackTraceElement
) {
    infix fun matches(sig: StackSignature) =
            caller == sig.caller &&
                    callee sameMethodAs sig.callee
}

infix fun StackTraceElement.sameMethodAs(other: StackTraceElement) =
        className  == other.className &&
                methodName == other.methodName

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

/**
 * Remove the part of the stacktrace starting from (not including) the given [StackSignature],
 * and everything below it, from this exception and all its cause exceptions.
 * @return A lambda that can be called to undo this action. You can e.g. shorten a stack, log it,
 *         then undo the shortening before rethrowing the exception.
 */
// TODO refactor to variable needle length
fun Throwable.removeStackFrom(sig: StackSignature): () -> Unit {

    // get and store once, because the getter makes a defensive copy
    val originalTrace = stackTrace

    // find the sig's caller in the exception stack
    val calleeIndex =
            originalTrace.asSequence().windowed(2).toList().findLastIndex {
                val windowSig = StackSignature(callee = it[0], caller = it[1])
                windowSig matches sig
            }

    // ignore not found, but also if found at 0
    if (calleeIndex > 0) {
        // snip!
        stackTrace = Arrays.copyOfRange(originalTrace, 0, calleeIndex + 1)
    }

    val thisUndoer = {
        stackTrace = originalTrace
    }

    // now recurse to cause
    when (val cauz = cause) {
        null -> return thisUndoer
        else -> {
            val causeUndoer: () -> Unit =
                    cauz.removeStackFrom(sig)

            return {
                causeUndoer()
                thisUndoer()
            }
        }
    }
}

/**
 * Don't use directly, use stackRoot()
 */
val currentStackBase = ThreadLocal<StackSignature?>()
// TODO @InlineOnly
@OptIn(ExperimentalContracts::class)
inline fun <T> stackRoot(body: () -> T): T {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    return currentStackBase.withValue(currentStackSignature()) { body() }
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
 * Iterates this exception and all its causes.
 */
val Throwable.causeChain: Iterable<Throwable> get() =
    independentIterable(this, Throwable::cause)

/**
 * Returns an [Iterable] that returns an iterator as specified in [independentIterator].
 */
fun <T> independentIterable(start: T, next: (cur: T) -> T?) =
    object : Iterable<T> {
        override fun iterator() = independentIterator(start, next)
    }

/**
 * Returns an iterator whose first element is [start],
 * which subsequently yields the result of calling [next],
 * until that returns `null`.
 */
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

/**
 * Return the string output of [printStackTrace].
 */
val Throwable.stackTraceString: String get() =
    StringWriter().also {
        printStackTrace(PrintWriter(it))
    }.toString()

/**
 * Runs [task] while doing its very best to ensure no exception ever propagates outside [noThrow].
 * - If [task] throws anything, calls [catcher] with the [Throwable].
 * - If that fails i.e. [catcher] throws, call [Throwable.printStackTrace] on the first exception.
 * - If even that fails, do nothing and return.
 */
inline fun noThrow(
    task: () -> Unit,
    catcher: (Throwable) -> Unit,
) {
    try {
        task()
    } catch (e: Throwable) {
        try {
            catcher(e)
        } catch (ee: Throwable) {
            try {
                e.addSuppressed(ee)
                e.printStackTrace()
            } catch (eee: Throwable) {
                // fine!
            }
        }
    }
}

/**
 * Runs [task] while doing its very best to ensure no exception ever propagates outside [noThrow].
 * - If [task] throws anything, call [Throwable.printStackTrace] on the exception.
 * - If even that fails, do nothing and return.
 */
inline fun noThrow(
    task: () -> Unit,
) {
    try {
        task()
    } catch (e: Throwable) {
        try {
            e.printStackTrace()
        } catch (eee: Throwable) {
            // fine!
        }
    }
}
