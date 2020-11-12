package org.jbali.threads

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty

// TODO initial
class ThreadLocalProperty<T : Any> : ReadWriteProperty<Any, T?> {
    private val v = ThreadLocal<T?>()
    override fun getValue(thisRef: Any, property: KProperty<*>): T? = v.get()
    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        v.set(value)
    }
}

inline fun <R, P> withPropAs(getter: () -> P, setter: (P) -> Unit, v: P, block: (P) -> R): R {
    val pre = getter()
    try {
        setter(v)
        return block(pre)
    } finally {
        setter(pre)
    }
}

/**
 * Call body while this threadlocal has value v.
 */
@OptIn(ExperimentalContracts::class)
inline fun <L, T> ThreadLocal<L?>.withValue(v: L, body: () -> T): T {
    contract {
        callsInPlace(body, InvocationKind.EXACTLY_ONCE)
    }
    val pre = get()
    try {
        set(v)
        return body()
    } finally {
        set(pre)
    }
}

inline fun <R, P> withPropAs(p: KMutableProperty0<P>, v: P, block: (P) -> R): R {
    val pre = p.get()
    try {
        p.set(v)
        return block(pre)
    } finally {
        p.set(pre)
    }
}

inline fun <T> runWithThreadName(name: String?, appendWithSeparator: String? = null, block: (String) -> T): T =
        Thread.currentThread().let { t ->
            val pre = t.name
            try {
                if (name != null) {
                    t.name = if (appendWithSeparator != null) "$pre$appendWithSeparator$name" else name
                }
                return block(pre)
            } finally {
                if (name != null) {
                    t.name = pre
                }
            }
        }

/**
 * If entered is true, returns inner()
 * if it's false, calls enter, which must invoke its argument (which is a wrapper around inner) and return that result.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> reentrant(entered: ThreadLocal<Boolean>, noinline inner: () -> T, wrapper: (inner: () -> T) -> T): T {
    contract {
        callsInPlace(inner, InvocationKind.EXACTLY_ONCE)
    }

    return if (entered.get()) inner()
    else {
        try {
            val thread = Thread.currentThread()

            entered.set(true)
            var innerCalled = false
            val res = wrapper {
                check(Thread.currentThread() == thread)
                check(!innerCalled)
                innerCalled = true
                inner()
            }
            check(innerCalled)
            res
        } finally {
            entered.set(false)
        }
    }
}

fun currentThreadList(atomic: Boolean = false): List<ThreadInfo> =
    Thread.getAllStackTraces().map { (thread, stack) ->
        if (atomic && thread != Thread.currentThread()) {

            // suspend and resume are deliberately used here despite their deprecation,
            // but forgot (to document) why exactly

            @Suppress("DEPRECATION")
            thread.suspend()
            try {
                ThreadInfo(
                        id = thread.id,
                        name = thread.name,
                        state = thread.state,
                        stack = thread.stackTrace.toList()
                )
            } finally {
                @Suppress("DEPRECATION")
                thread.resume()
            }
        } else {
            ThreadInfo(
                    id = thread.id,
                    name = thread.name,
                    state = thread.state,
                    stack = stack.toList()
            )
        }
    }

val Thread.State?.letter: Char get() =
    when (this) {
        Thread.State.NEW -> 'N'
        Thread.State.RUNNABLE -> 'R'
        Thread.State.BLOCKED -> 'B'
        Thread.State.WAITING -> 'W'
        Thread.State.TIMED_WAITING -> 'w'
        Thread.State.TERMINATED -> 'T'
        null       -> '?'
    }

data class ThreadInfo(
        val id: Long,
        val state: Thread.State,
        val name: String,
        val stack: List<StackTraceElement>
)
