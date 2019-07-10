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
val Enum<*>.jpql get() = fullname

// now wouldn't it be nice if Kotlin could just inline this as a constexpr?
inline fun <reified T : Enum<T>> jpaEnum(e: T) = T::class.qualifiedName + "." + e.name

/**
 * Run action on each item in this iterable. If an exception occurs while processing an item,
 * wrap it in an exception that has the item's toString in the message.
 */
fun <T> Iterable<T>.forEachWrappingExceptions(action: (T) -> Unit) {
    for (x in this) {
        try {
            action(x)
        } catch (e: Throwable) {
            val xs =
                    try {
                        x.toString()
                    } catch (tse: Throwable) {
                        "{!{toString error: $tse}!}"
                    }
            throw RuntimeException("Exception while processing item $xs: $e", e)
        }
    }
}

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

class OutVar<T : Any> {
    lateinit var value: T
    override fun toString() = value.toString()
    override fun equals(other: Any?) = if (other is OutVar<*>) value == other.value else value == other
    override fun hashCode() = value.hashCode()
}

data class ExplainedBool @JvmOverloads constructor(
        val value: Boolean,
        val explanation: String = "byDefault"
) {
    constructor(value: Boolean, explanationRequested: Boolean, explainer: () -> String) :
            this(
                    value = value,
                    explanation = if (explanationRequested) explainer() else "<explanation not requested>"
            )

    override fun toString() = "${value.toString().padEnd(5)} because $explanation"

    /** Because `if (foo().isTrue())` reads better than `if (foo().value)` */
    fun isTrue() = value
    fun isFalse() = !value

    /**
     * Allows the following usage pattern:
     * ```
     * var exp = OutVar<String>()
     * return
     *   if (explainedFoo().isFalse(exp)) false because "foo said $exp"
     *   else (explainedBar().isTrue(exp)) true because "bar said $exp"
     *   else true because "why not"
     * }
     * ```
     */
    fun isTrue(outExplanation: OutVar<String>): Boolean {
        outExplanation.value = explanation
        return value
    }

    /**
     * See isTrue for doc in reverse
     */
    fun isFalse(outExplanation: OutVar<String>): Boolean {
        outExplanation.value = explanation
        return !value
    }

}

infix fun Boolean.because(explanation: String) = ExplainedBool(this, explanation)
