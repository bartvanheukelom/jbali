package org.jbali.util

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Wrapper for a [KType] that adds a compile-time type parameter [T].
 * Currently only supports that the type is an actual class (not a type parameter).
 * Construct using [classedTypeOf].
 */
data class ClassedType<T : Any>(
        val type: KType
) {

    @Suppress("UNCHECKED_CAST")
    val clazz get() =
        type.classifier as KClass<T>

    companion object {
        @ExperimentalStdlibApi
        val unit = classedTypeOf<Unit>()
    }
}

@ExperimentalStdlibApi
inline fun <reified T : Any> classedTypeOf(): ClassedType<T> =
        ClassedType(type = typeOf<T>())
