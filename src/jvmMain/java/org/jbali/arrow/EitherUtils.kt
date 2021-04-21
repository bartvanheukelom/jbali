package org.jbali.arrow

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import arrow.core.left
import arrow.core.right

/**
 * @return `Right(this)`, or `Left(default())` if `this` is `null`.
 */
fun <A, B> B?.nullToLeftOf(default: () -> A): Either<A, B> =
    this?.right() ?: default().left()

/**
 * Return the actual value in this either, whether it is left or right.
 * @param C The common supertype of the left and right type.
 */
fun <C> Either<C, C>.value() = fold({it},{it})

/**
 * @return the right value if this is [Right].
 * @throws A the left value if this is [Left].
 */
fun <A : Throwable, B> Either<A, B>.getOrThrow(): B =
        when (this) {
            is Left  -> throw value
            is Right ->       value
        }
/**
 * @return the right value if this is [Right].
 * @throws T if this is [Left], the result of passing the left value to [exceptionMaker].
 */
fun <A, B, T : Throwable> Either<A, B>.getOrThrow(exceptionMaker: (A) -> T): B =
        when (this) {
            is Left  -> throw exceptionMaker(value)
            is Right ->                      value
        }
