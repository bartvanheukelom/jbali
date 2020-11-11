package org.jbali.reflect

import org.jbali.util.invocationToString
import java.lang.reflect.Method
import kotlin.reflect.jvm.kotlinFunction

/**
 * Represents an invocation of the Java [Method] given in [method] with [args].
 *
 * Note that [equals] and [hashCode] for this class are defined using equality
 * of these arguments (as per data class default).
 * Two distinct invocations, performed at different times or by different callers, may therefore compare equal.
 */
data class MethodInvocation(
        val method: Method,
        val args: List<Any?>
) {

    val invokesToString get() =
        method.name == "toString" && args.isEmpty()

    constructor(method: Method, args: Array<*>) :
            this(method, args.toList())

    override fun toString() =
            when (val f = method.kotlinFunction) {
                null -> Proxies.invocationToString(method, args.toTypedArray())
                else -> invocationToString(f, *args.toTypedArray())
            }

}
