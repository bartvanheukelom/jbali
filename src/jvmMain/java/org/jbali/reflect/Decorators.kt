package org.jbali.reflect

import org.slf4j.Logger
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction


/**
 * Faux constructor for [DecoratorBuilder] using reified [TIface] as its interface argument.
 */
@Suppress("FunctionName") // faux constructor
inline fun <reified TIface : Any> DecoratorBuilder(impl: TIface) = DecoratorBuilder(TIface::class, impl)

/**
 * Create a [DecoratorBuilder] for [TIface] using the receiver as delegate.
 */
inline fun <reified TIface : Any> TIface.decorate() = DecoratorBuilder(TIface::class, this)

/**
 * Builder for a decorator proxy which implements the given interface
 * and in some way delegates method calls to the given implementation.
 *
 * What behaviour they add on top of that depends on which [DecoratorBuilder] method is called.
 *
 * A [DecoratorBuilder] is stateless and could potentially be used to create multiple proxies
 * for the same interface/implementation combination.
 */
class DecoratorBuilder<TIface : Any>(
        private val iface: KClass<TIface>,
        private val impl: TIface
) {

    /**
     * Create a proxy which logs each invocation to [log], optionally prefixed with [prefix], before delegating.
     */
    fun loggingBefore(log: Logger, prefix: String = "") = withBefore { log.info("$prefix$it") }

    /**
     * Create a proxy which calls [before], before delegating.
     */
    fun withBefore(before: (MethodInvocation) -> Unit): TIface {
        val type = iface.java
        val impl2 = impl // let lambda capture only what it needs

        // TODO use new functions
        return type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf<Class<*>>(type)) { _, method, args ->
            before.invoke(MethodInvocation(method, args ?: arrayOf<Any?>()))
            Proxies.invokeTransparent(method, impl2, args)
        })
    }

}
