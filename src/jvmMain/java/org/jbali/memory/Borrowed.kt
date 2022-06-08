package org.jbali.memory

import java.lang.ref.WeakReference

/**
 * When a [Borrowed] is given to a function or constructor, it's implied that the receiver
 * will cause a new (strong) reference to the value inside it to be stored on the heap.
 *
 * However, do note that this is not enforceable at compile time or runtime.
 *
 * The name of this class was inspired by Rust's borrow checker, but it probably works rather differently.
 */
@JvmInline
value class Borrowed<T>

// TODO:
//@PublishedApi
//internal constructor
(
    private val v: Any?
) {

    @Suppress("UNCHECKED_CAST")
    fun get(): T = v as T

    operator fun invoke() = get()
    operator fun component1() = get()

    /**
     * Simply returns the value inside, just like [invoke],
     * but calling this communicates that the caller is intentionally violating
     * the contract of [Borrowed], i.e. will store a new reference to the value on the heap.
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("stealing is bad")
    fun steal(): T = get()

    /**
     * Create and return a new [WeakReference] to the borrowed object, which is allowed.
     */
    fun weakReference() =
        WeakReference(invoke())
}

fun <T> loan(v: T) = Borrowed<T>(v)
@JvmName("loanExt")
fun <T> T.loan() = Borrowed<T>(this)
