package org.jbali.service

import java.lang.reflect.Method
import kotlin.reflect.KType
import kotlin.reflect.jvm.kotlinFunction

fun Method.returnKType(): KType =
    (this.kotlinFunction ?: throw IllegalArgumentException("Can't get kotlinFunction for $this"))
        .returnType
