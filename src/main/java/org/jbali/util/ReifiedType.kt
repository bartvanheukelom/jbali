package org.jbali.util

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Wrapper for a [KType] that adds a compile-time type parameter [T], representing the same type.
 * Currently only supports that the type is an actual class (not a type parameter).
 * Construct using [reifiedTypeOf].
 */
data class ReifiedType<T : Any>(
        val type: KType
) {

    @Suppress("UNCHECKED_CAST")
    val clazz get() =
        type.classifier as KClass<T>

    companion object {
        val unit = reifiedTypeOf<Unit>()
    }
}


inline fun <reified T : Any> reifiedTypeOf(): ReifiedType<T> {

    // can safely opt in because this is going to be stable in 1.4:
    //     https://blog.jetbrains.com/kotlin/2019/12/what-to-expect-in-kotlin-1-4-and-beyond/#language-features
    @OptIn(ExperimentalStdlibApi::class)
    val type = typeOf<T>()

    return ReifiedType(type = type)
}
