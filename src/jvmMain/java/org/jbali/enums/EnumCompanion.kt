package org.jbali.enums

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

    // TODO Kotlin EnumSet which overrides operators to results in EnumSets
    val none: Set<E> = EnumSet.noneOf(javaClass)
    val all: Set<E> = EnumSet.allOf(javaClass)
    fun setOf(vararg values: E): Set<E> =
            EnumSet.noneOf(javaClass).apply {
                addAll(values)
            }
    
    /**
     * Map of all enum constants by their lowercased name.
     */
    val byLowerName: Map<String, E> = all.associateBy { it.name.toLowerCase() }

    /**
     * Find the enum constant with the given name, ignoring case and trimming outer whitespace.
     * @throws IllegalArgumentException if there is no such constant.
     */
    fun parse(name: String): E =
        parseOrNull(name)
            ?: throw IllegalArgumentException("No enum constant ${javaClass.name}.${name}")
    
    /**
     * Find the enum constant with the given name, ignoring case and trimming outer whitespace.
     * @return the constant, or `null` if none found.
     */
    fun parseOrNull(name: String) = byLowerName[name.trim().toLowerCase()]
    
}

@Suppress("FunctionName") // faux constructor
inline fun <reified E : Enum<E>> EnumTool() =
        EnumCompanion(E::class)
