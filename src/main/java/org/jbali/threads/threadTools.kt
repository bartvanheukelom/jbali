package org.jbali.threads

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KMutableProperty0

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

inline fun <T> withThreadName(name: String, block: (String) -> T) =
        Thread.currentThread().let {
            withPropAs(
                    it::getName,
                    it::setName,
                    name,
                    block
            )
        }

inline fun <T> reentrant(entered: ThreadLocal<Boolean>, inner: () -> T, enter: () -> T) =
        if (entered.get()) inner()
        else {
            try {
                entered.set(true)
                enter()
            } finally {
                entered.set(false)
            }
        }
