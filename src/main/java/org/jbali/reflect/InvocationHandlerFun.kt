package org.jbali.reflect

import java.lang.reflect.InvocationHandler

typealias InvocationHandlerFun = ((MethodInvocation) -> Any?)

fun InvocationHandlerFun.asInvocationHandler() =
        InvocationHandler { _, method, args ->
            val mi = MethodInvocation(method, args ?: arrayOf())
            this(mi)
        }
