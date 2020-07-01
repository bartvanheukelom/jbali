import arrow.core.Either
import arrow.core.left
import arrow.core.right

fun <A, B> B?.nullToLeft(default: () -> A): Either<A, B> =
        when (this) {
            null -> default().left()
            else -> this.right()
        }

/**
 * Return the actual value in this either, whether it is left or right.
 * @param C The common supertype of the left and right type.
 */
fun <C> Either<C, C>.value() = fold({it},{it})
