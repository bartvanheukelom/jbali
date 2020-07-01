package org.jbali.arrow

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import arrow.core.getOrHandle

inline class ErrorMessage(val msg: String)

/**
 * Represents the result of a call that may give
 * an error message instead of a return value.
 */
typealias Uncertain<T> = Either<ErrorMessage, T>

/**
 * Returns the certain value, or throws the error.
 * @throws IllegalStateException with the [ErrorMessage] as message.
 */
fun <T> Uncertain<T>.getOrThrow(): T =
        getOrHandle {
            throw IllegalStateException(it.msg)
        }

/**
 * `Right(this)` if not null, else `Left(ErrorMessage(errMsg()))`.
 */
fun <T> T?.nullToError(errMsg: () -> String): Either<ErrorMessage, T> =
        when (this) {
            null -> Left(ErrorMessage(errMsg()))
            else -> Right(this)
        }
