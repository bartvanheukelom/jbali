package org.jbali.util

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

/**
 * Wrapper for a [KType] that adds a compile-time type parameter [T], representing the same type.
 * Currently only supports that the type is an actual class (not a type parameter).
 * Construct using [reifiedTypeOf].
 */
data class ReifiedType<T>(
        val type: KType
) {

    val javaType get() = type.javaType

//    @Suppress("UNCHECKED_CAST")
//    val clazz: KClass<T> get() = type.classifier
//
//        listOf<T>().filterNotNull
//
//        type.classifier as KClass<T>

    companion object {
        val unit = reifiedTypeOf<Unit>()
    }

    fun match() = Matcher<T>(this)

    data class Matcher<T>(
            /** Is public for inlining, but do not use directly. */
            val thisType: ReifiedType<T>,
            /** Is public for inlining, but do not use directly. */
            val result: Box<T>? = null
    ) {

        inline fun <reified O> to(block: () -> O): Matcher<T> =
                to(reifiedTypeOf<O>(), block)

        inline fun <O> to(m: ReifiedType<O>, block: () -> O): Matcher<T> =
                when {

                    result != null ->
                        // previous match was a success, keep returning this until a terminator is called
                        this

                    m extends thisType ->
                        // unchecked cast because compiler doesn't know that O extends T, but we do
                        @Suppress("UNCHECKED_CAST")
                        this.copy(result = Box(block()) as Box<T>)

                    else ->
                        this
                }

        /**
         * Terminates the matcher by returning the result of the matched block, or throwing [IllegalArgumentException].
         */
        fun orThrow(): T =
                when {
                    result != null -> result.contents
                    else -> throw IllegalArgumentException("No match for type $thisType")
                }

        /**
         * Terminates the matcher by returning the result of the matched block, or the given [block].
         */
        inline fun otherwise(block: () -> T): T =
                when {
                    result != null -> result.contents
                    else -> block()
                }

    }

    infix fun supers(m: ReifiedType<*>): Boolean =
            type.isSupertypeOf(m.type)

    infix fun extends(m: ReifiedType<*>): Boolean =
            type.isSubtypeOf(m.type)

}

inline fun <reified T> KType.reify(): ReifiedType<T> {
    val parent = reifiedTypeOf<T>()
    require (this.isSubtypeOf(parent.type)) {
        "$this is not a subtype of $parent"
    }
    @Suppress("UNCHECKED_CAST")
    return cachedReifiedType as ReifiedType<T>
}

fun KType.match(): ReifiedType.Matcher<Any?> =
        ReifiedType<Any?>(this).match()


// TODO there must be a way to merge these two
@Suppress("UNCHECKED_CAST")
fun <T : Any> ReifiedType<T?>.realClassOfNullable(): KClass<T> =
        type.classifier as KClass<T>
@Suppress("UNCHECKED_CAST")
fun <T : Any> ReifiedType<T>.realClass(): KClass<T> =
        type.classifier as KClass<T>

inline fun <reified T> reifiedTypeOf(): ReifiedType<T> {

    // can safely opt in because this is going to be stable in 1.4:
    //     https://blog.jetbrains.com/kotlin/2019/12/what-to-expect-in-kotlin-1-4-and-beyond/#language-features
    @OptIn(ExperimentalStdlibApi::class)
    val type = typeOf<T>()

    @Suppress("UNCHECKED_CAST")
    return type.cachedReifiedType as ReifiedType<T>
}

val KType.cachedReifiedType: ReifiedType<*> by StoredExtensionProperty {
    ReifiedType<Any?>(type = this)
}