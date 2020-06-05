package org.jbali.kotser

import kotlinx.serialization.*

/**
 * Base class for a [KSerializer] for [T] which serializes values
 * by converting them from/to a string.
 */
abstract class StringBasedSerializer<T> : KSerializer<T> {

    override val descriptor = PrimitiveDescriptor(javaClass.name, PrimitiveKind.STRING)

    final override fun deserialize(decoder: Decoder): T =
            fromString(decoder.decodeString())

    final override fun serialize(encoder: Encoder, value: T) =
            encoder.encodeString(toString(value))

    abstract fun fromString(s: String): T

    /**
     * Convert [o] to the string that is serialized.
     *
     * Defaults to `o.toString()`.
     */
    open fun toString(o: T): String = o.toString()

}
