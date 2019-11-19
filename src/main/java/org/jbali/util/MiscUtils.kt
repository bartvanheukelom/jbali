package org.jbali.util

import arrow.core.Either
import org.slf4j.Logger
import java.io.Serializable
import java.security.SecureRandom
import java.time.Duration
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlValue
import kotlin.reflect.KCallable
import kotlin.reflect.full.valueParameters

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

/**
 * Return the actual value in this either, whether it is left or right.
 * @param C The common supertype of the left and right type.
 */
fun <C> Either<C, C>.value() = fold({it},{it})

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
            throw RuntimeException("While processing item $xs: $e", e)
        }
    }
}

fun <T> Iterable<T>.forEachCatching(
        errorHandler: (T, Throwable) -> Unit, // TODO simple logging version
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
fun Boolean.byDefault() = ExplainedBool(this)

/** The length of this duration in seconds, as double, with millisecond precision. */
val Duration.secondsDouble get() = toMillis().toDouble() / 1000.0

fun Logger.invocation(func: KCallable<*>, vararg args: Any?) {
    info(invocationToString(func, *args))
}

fun invocationToString(func: KCallable<*>, vararg args: Any?): String {
    val argsStringed = args.mapIndexed { i, v ->
        func.valueParameters.getOrNull(i)?.name + " = " + v.toString()
    }
    val argsJoined =
            if (argsStringed.sumBy { it.length } >= 120) argsStringed.joinToString(separator = "\n\t", prefix = "\n\t", postfix = "\n")
            else argsStringed.joinToString()
    val s = "${func.name}($argsJoined)"
    return s
}

/** Format millis to MMM:SS:mmm */
fun formatMsTime(time: Long): String {
    val d = Duration.ofMillis(time)
    val min = d.toMinutes()
    val sec = d.seconds - (min * 60)
    val ms = d.nano / 1_000_000L
    return String.format("%03d:%02d:%03d", min, sec, ms)
}

// TODO support T-
fun formatTTime(time: Long): String = "T+${formatMsTime(time)}"


/**
 * Can be used to store a password or other secret, prevents accidentally
 * printing/logging them or passing them where a non-secret string is accepted.
 *
 * Contains no countermeasures against runtime (memory) hacks or anything like that!
 */
@XmlAccessorType(XmlAccessType.FIELD)
data class Password(@field:XmlValue private val value: String) : Serializable {

    @Suppress("unused") private constructor() : this(stringToBeUnmarshalled)

    /**
     * Returns the value.
     * The name of this getter is intentionally verbose.
     */
    fun accessPasswordValue(): String = value

    override fun toString() = "Password(*****)"

    companion object {
        const val serialVersionUID = 1L
    }

}

/**
 * Assign to string properties of a default-constructed object that are soon to be given
 * their real value by some kind of deserialization system.
 */
const val stringToBeUnmarshalled = "stringToBeUnmarshalled"
