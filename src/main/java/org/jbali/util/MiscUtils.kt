package org.jbali.util

import org.slf4j.Logger
import java.security.SecureRandom
import java.time.Duration
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.cast
import kotlin.reflect.full.valueParameters

val globalSecureRandom = SecureRandom()


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
 * Run action on each item in this iterable. If an exception occurs while processing an item,
 * wrap it in an exception that has the item's toString in the message.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> Iterable<T>.forEachWrappingExceptions(action: (T) -> Unit) {
    contract {
        callsInPlace(action, InvocationKind.UNKNOWN)
    }
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

/**
 * Normally returns the result of block().
 * If block() throws, the exception is swallowed, and fb is returned instead.
 */
inline fun <T> withFallback(fb: T, block: () -> T) =
        try {
            block()
        } catch (e: Throwable) {
            fb
        }

class OutVar<T : Any> {
    lateinit var value: T
    override fun toString() = value.toString()
    override fun equals(other: Any?) = if (other is OutVar<*>) value == other.value else value == other
    override fun hashCode() = value.hashCode()
}

fun Logger.invocation(func: KCallable<*>, vararg args: Any?) {
    info(invocationToString(func, *args))
}

val KType.simplified: String by StoredExtensionProperty {
    val str = this.toString()
    if (str.count { it == '.' } == 1) {
        str.removePrefix("kotlin.")
    } else {
        str
    }
}

val KClass<*>.kotlinReferenceLink: String by StoredExtensionProperty {
    "https://kotlinlang.org/api/latest/jvm/stdlib/" +
            (qualifiedName ?: "kotlin.Nothing").let {
                val ld = it.lastIndexOf('.')
                val pack = it.substring(0, ld)
                val name = it.substring(ld + 1)
                "$pack/-${name.toLowerCase()}"
            } + "/"
}

val Pair<*, *>.assignFormatted get() = "${first}=${second}"

/**
 * ```
 * fun foo(x: String, y: Int)
 * invocationToString(::foo, "bar", 12, "derp")
 * // = "foo(x=bar, y=12, [2]=derp)"
 * ```
 *
 * @see org.jbali.reflect.MethodInvocation
 */
fun invocationToString(func: KCallable<*>, vararg args: Any?): String {

    // TODO loop on max(len(params), len(args)) to also display when too few args passed
    val argsStringed = args.mapIndexed { i, v ->
        val param = func.valueParameters.getOrNull(i)?.name ?: "[$i]"
        val arg = v.toString()
        "$param=$arg"
    }

    val argsJoined =
            if (argsStringed.sumBy { it.length } >= 120) argsStringed.joinToString(separator = "\n\t", prefix = "\n\t", postfix = "\n")
            else argsStringed.joinToString()

    return "${func.name}($argsJoined)"
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
 * Assign to string properties of a default-constructed object that are soon to be given
 * their real value by some kind of deserialization system.
 */
const val stringToBeUnmarshalled = "stringToBeUnmarshalled"

infix fun <A, B : Any> A.asClass(to: KClass<B>) =
        to.cast(this)

inline fun <reified T : Any> Any.cast() = this as T

/**
 * Very simple wrapper around any value.
 * Can for instance be used to have nested nullability, i.e. a `Box<Int?>?` to distinguish between:
 * - `null`
 * - `Box(null)`
 * - `Box(12)`
 *
 * A box is equal to another box if the contents are also equal. This is in contrast to [ObjectIdentity].
 */
data class Box<out T>(val contents: T)


/**
 * Wraps an object giving it identity-based equals and hashCode,
 * even if it normally has value-based versions of them.
 */
class ObjectIdentity(val o: Any) {
    override fun equals(other: Any?) = other is ObjectIdentity && other.o === o
    override fun hashCode() = System.identityHashCode(o)
}

fun ByteArray.toHexString(limit: Int = size): String =
        HexBytes.toHex(this, limit)

/**
 * Basically:
 *
 *     (getter() ?: generator())
 *         .updater()
 *         .also(setter)
 */
inline fun <T> genericUpdate(
        getter: () -> T?,
        generator: () -> T,
        updater: T.() -> T,
        setter: (T) -> Unit
): T =
        (getter() ?: generator())
                .updater()
                .also(setter)
