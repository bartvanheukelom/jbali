package org.jbali.reflect

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

typealias InvocationHandlerFun = ((MethodInvocation) -> Any?)

/**
 * Present a function that takes a [MethodInvocation] as an [InvocationHandler].
 */
@Suppress("ObjectLiteralToLambda")
fun InvocationHandlerFun.asInvocationHandler() =
        object : InvocationHandler {
            override fun invoke(
                    proxy: Any,
                    method: Method,
                    args: Array<out Any?>?
            ): Any? {
                val mi = MethodInvocation(method, args?.toList() ?: emptyList())
                return this@asInvocationHandler(mi)
            }
        }

