package org.jbali.util

import arrow.core.Either
import java.security.SecureRandom

val globalSecureRandom = SecureRandom()

enum class SortOrder(
        /** 1 for ASCENDING, -1 for DESCENDING */
        val multiplier: Int
) {

    ASCENDING(1),
    DESCENDING(-1);

    fun <T : Comparable<T>> comparator(): Comparator<T> = when (this) {
        SortOrder.ASCENDING -> naturalOrder()
        SortOrder.DESCENDING -> reverseOrder()
    }
}


/**
 * Return a function 'f_once' that will invoke f,
 * only the first time f_once is invoked.
 *
 * Using f_once from multiple threads, or throwing from f,
 * triggers undefined behaviour.
 */
fun onceFunction(f: () -> Unit): () -> Unit {
    var invoked = false
    return {
        if (!invoked) {
            invoked = true
            f()
        }
    }
}

fun <A,B> Either<A,B>.any() = fold({it},{it})

/**
 * The fully qualified name of this enum constant, e.g. foo.bar.Color.RED
 */
val Enum<*>.fullname get() = javaClass.canonicalName + "." + name

// now wouldn't it be nice if Kotlin could just inline this as a constexpr?
inline fun <reified T : Enum<T>> jpaEnum(e: T) = T::class.qualifiedName + "." + e.name

fun <T> Iterable<T>.forEachCatching(
        errorHandler: (T, Throwable) -> Unit,
        action: (T) -> Unit
) {
    for (x in this) {
        try {
            action(x)
        } catch (e: Throwable) {
            try {
                errorHandler(x, e)
            } catch (ee: Throwable) {
                try {
                    ee.printStackTrace()
                } catch (eee: Throwable) {
                    // shrug
                }
            }
        }
    }
}
