package org.jbali.reflect

import org.jbali.util.invocationToString
import org.slf4j.Logger
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction

class MethodInvocation(
        val method: Method,
        val args: Array<Any?>
) {
    override fun toString() =
            method.kotlinFunction?.let { invocationToString(it, *args) }
                ?: Proxies.invocationToString(method, args)
}

@Suppress("FunctionName") // pseudo-constructor
inline fun <reified TIface : Any> DecoratorBuilder(impl: TIface) = DecoratorBuilder(TIface::class, impl)

inline fun <reified TIface : Any> TIface.decorate() = DecoratorBuilder(TIface::class, this)

class DecoratorBuilder<TIface : Any>(
        private val iface: KClass<TIface>,
        private val impl: TIface
) {

    fun loggingBefore(log: Logger, prefix: String = "") = withBefore { log.info("$prefix$it") }

    fun withBefore(before: (MethodInvocation) -> Unit): TIface {
        val type = iface.java
        val impl2 = impl // let lambda capture only what it needs
        return type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf<Class<*>>(type)) { _, method, args ->
            before.invoke(MethodInvocation(method, args ?: arrayOf()))
            Proxies.invokeTransparent(method, impl2, args)
        })
    }

}