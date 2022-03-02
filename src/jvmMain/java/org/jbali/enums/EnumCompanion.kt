package org.jbali.enums

import com.google.common.base.Enums
import java.util.*
import kotlin.reflect.KClass

/**
 * Wrapper for the [KClass] that reflects an enum class.
 * Exposes various "extensions" for that enum class, because it's
 * currently not possible to directly add static extensions to such a type.
 *
 * The easiest way to use [EnumCompanion] is to extend the companion object of an
 * enum class from it, but if that's not possible, you can also construct an instance
 * separately.
 *
 * Example:
 *
 *     enum class Colour {
 *         Red, Green, Blue;
 *         companion object : EnumCompanion<Colour>(Colour::class)
 *     }
 *
 *     val boring: EnumSet<Colour> = Colour.none
 */
open class EnumCompanion<E : Enum<E>>(
        val enum: KClass<E>
) {

    val javaClass = enum.javaObjectType
    
    fun valueOf(name: String): E =
        Enums.getIfPresent(javaClass, name).orNull()
            ?: throw IllegalArgumentException("No enum constant ${javaClass.name}.${name}")

    // TODO Kotlin EnumSet which overrides operators to results in EnumSets
    val none: Set<E> = EnumSet.noneOf(javaClass)
    val all: Set<E> = EnumSet.allOf(javaClass)
    fun setOf(vararg values: E): Set<E> =
            EnumSet.noneOf(javaClass).apply {
                addAll(values)
            }

}

@Suppress("FunctionName") // faux constructor
inline fun <reified E : Enum<E>> EnumTool() =
        EnumCompanion(E::class)
