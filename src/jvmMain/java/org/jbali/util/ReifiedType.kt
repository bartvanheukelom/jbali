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

    override fun toString() =
            type.toString()

//    @Suppress("UNCHECKED_CAST")
//    val clazz: KClass<T> get() = type.classifier
//
//        listOf<T>().filterNotNull
//
//        type.classifier as KClass<T>

    companion object {
        val unit = reifiedTypeOf<Unit>()
    }

    fun match() = Matcher<T, Unit>(this, Unit)

    fun <I : Any?> match(input: I) = Matcher<T, I>(this, input)

    data class Matcher<T, I>(
            /** Is public for inlining, but do not use directly. */
            val thisType: ReifiedType<T>,
            /** Is public for inlining, but do not use directly. */
            val input: I,
            /** Is public for inlining, but do not use directly. */
            val result: Box<T>? = null
    ) {
        
        /**
         * Matches if [thisType] supers [O].
         */
        inline fun <reified O> to(block: (I) -> O): Matcher<T, I> =
                to(reifiedTypeOf<O>(), block)

        /**
         * Matches if [thisType] supers [m].
         */
        inline fun <O> to(m: ReifiedType<O>, block: (I) -> O): Matcher<T, I> =
                when {

                    result != null ->
                        // previous match was a success, keep returning this until a terminator is called
                        this

                    thisType supers m ->
                        // unchecked cast because compiler doesn't know that T extends O, but we do
                        @Suppress("UNCHECKED_CAST")
                        this.copy(result = Box(block(input)) as Box<T>)

                    else ->
                        this
                }
        
        /**
         * Matches if [thisType] extends [O].
         */
        inline fun <reified O> extends(block: (I) -> O): Matcher<T, I> =
                extends(reifiedTypeOf<O>(), block)
        
        /**
         * Matches if [thisType] extends [m].
         */
        inline fun <O> extends(m: ReifiedType<O>, block: (I) -> O): Matcher<T, I> =
                when {

                    result != null ->
                        // previous match was a success, keep returning this until a terminator is called
                        this

                    thisType extends m ->
                        // unchecked cast because compiler doesn't know that T extends O, but we do NO WE DON'T BUT HOW TO SOLVE THIS AAARGH
                        @Suppress("UNCHECKED_CAST")
                        this.copy(result = Box(block(input)) as Box<T>)

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
        inline fun otherwise(block: (I) -> T): T =
                when {
                    result != null -> result.contents
                    else -> block(input)
                }

    }

    infix fun supers(m: ReifiedType<*>): Boolean =
            type.isSupertypeOf(m.type)

    infix fun extends(m: ReifiedType<*>): Boolean =
            type.isSubtypeOf(m.type)

}

/**
 * Tell the compiler that this [KType] represents (a subtype of) [T],
 * verified at runtime.
 * @throws ClassCastException if this is not [T].
 */
inline fun <reified T> KType.reify(): ReifiedType<T> {

    val parent = reifiedTypeOf<T>()
    if (!this.isSubtypeOf(parent.type)) {
        throw ClassCastException("($this).reify<$parent>() invalid: $this is not a subtype of $parent")
    }

    return ReifiedType(this)
}

fun KType.match(): ReifiedType.Matcher<Any?, Unit> =
        ReifiedType<Any?>(this).match()


// TODO there must be a way to merge these two
@Suppress("UNCHECKED_CAST")
fun <T : Any> ReifiedType<T?>.realClassOfNullable(): KClass<T> =
        type.classifier as KClass<T>
@Suppress("UNCHECKED_CAST")
fun <T : Any> ReifiedType<T>.realClass(): KClass<T> =
        type.classifier as KClass<T>

inline fun <reified T> reifiedTypeOf(): ReifiedType<T> {

    // can safely opt in because this is going to be stable in 1.4 (update: on 1.5.0-RC and it still isn't...):
    //     https://blog.jetbrains.com/kotlin/2019/12/what-to-expect-in-kotlin-1-4-and-beyond/#language-features
    @OptIn(ExperimentalStdlibApi::class)
    val type = typeOf<T>()

    return ReifiedType(type)
}
