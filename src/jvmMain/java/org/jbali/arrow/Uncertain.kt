package org.jbali.arrow

import arrow.core.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

@JvmInline
value class ErrorMessage(val msg: String) {
    override fun toString(): String = "Error: $msg"
}

/**
 * Represents the result of a call that may give
 * an error message instead of a return value.
 *
 * The actual type is [Either],
 * where a [Left] is called the [errorMessage],
 * and a [Right] is called the [result].
 *
 * Utility functions related to this usage are:
 * - [getOrThrowAs]
 * - [nullToError]
 */
typealias Uncertain<T> = Either<ErrorMessage, T>

/**
 * If this is certain, run [block] with the value.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> Uncertain<T>.ifCertain(crossinline block: (T) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if (this is Either.Right) {
        block(this.value)
    }
}

/**
 * Returns the [toString] of the certain value, or the error message prefixed by "Error: ".
 * This can theoretically lead to a disambiguity if the value's [toString] also includes said prefix.
 */
fun Uncertain<*>.toStringOrError(): String =
        value().toString()

/**
 * Returns the result, or throws the error.
 * @throws IllegalStateException with the [ErrorMessage] as message.
 */
fun <T> Uncertain<T>.getOrThrow(): T =
        getOrHandle {
            throw IllegalStateException(it.msg)
        }


/**
 * `Right(this)` if not null, else `Left(ErrorMessage(errMsg()))`.
 */
inline fun <T> T?.nullToError(errMsg: () -> String): Uncertain<T> =
        when (this) {
            null -> errorMessage(errMsg())
            else -> result(this)
        }

@Deprecated("No need to call this on a non-nullable type", ReplaceWith("result(this)"))
@JvmName("nullToError\$rcvNotNull")
@JvmSynthetic
@Suppress("UNUSED_PARAMETER")
fun <T : Any> T.nullToError(noErrMsg: () -> String): Uncertain<T> =
        result(this)


fun <T> result(r: T) =
    r.right()

fun errorMessage(msg: String) =
    ErrorMessage(msg).left()
