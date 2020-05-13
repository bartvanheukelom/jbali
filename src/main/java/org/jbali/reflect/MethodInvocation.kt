package org.jbali.reflect

import org.jbali.util.invocationToString
import java.lang.reflect.Method
import kotlin.reflect.jvm.kotlinFunction

/**
 * A JVM method invocation, e.g. as intercepted by a [java.lang.reflect.Proxy].
 */
class MethodInvocation(
        val method: Method,
        val args: Array<Any?>
) {

    val invokesToString get() =
        method.name == "toString" && args.isEmpty()

    override fun toString() =
            method.kotlinFunction?.let { invocationToString(it, *args) }
                ?: Proxies.invocationToString(method, args)

}
