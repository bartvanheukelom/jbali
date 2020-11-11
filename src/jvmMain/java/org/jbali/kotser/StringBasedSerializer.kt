package org.jbali.kotser

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.reflect.KClass

/**
 * Base class for a [KSerializer] for [T] which serializes values
 * by converting them from/to a string.
 */
abstract class StringBasedSerializer<T : Any>(
        type: KClass<T>
) : TransformingSerializer<T, String>(
        type = type,
        backend = String.serializer()
) {

    abstract fun fromString(s: String): T

    /**
     * Convert [o] to the string that is serialized.
     *
     * Defaults to `o.toString()`.
     */
    open fun toString(o: T): String = o.toString()

    override fun transform(obj: T) = toString(obj)
    override fun detransform(tf: String): T = fromString(tf)

}
