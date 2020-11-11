package org.jbali.reflect

import java.lang.reflect.Proxy


inline fun <reified I : Any> createProxy(
        classLoader: ClassLoader? = null,
        noinline invocationHandler: InvocationHandlerFun
): I =
        Proxy.newProxyInstance(
                classLoader ?: I::class.java.classLoader,
                arrayOf<Class<*>>(I::class.java),
                invocationHandler.asInvocationHandler()
        ) as I
