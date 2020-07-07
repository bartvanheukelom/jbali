package org.jbali.kotser

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlin.reflect.KClass


/**
 * Base class for a [KSerializer] for [T] which serializes values
 * by converting them from/to [B] using the abstract methods [transform] and [detransform],
 * and delegating the actual serialization to the given [backend] serializer.
 */
// TODO make interface Transformer<T, F>, and a variant of this class which accepts an instance
abstract class TransformingSerializer<T : Any, B>(
        val type: KClass<T>,
        val backend: KSerializer<B>
) : KSerializer<T> {

    abstract fun transform(obj: T): B
    abstract fun detransform(tf: B): T

    override val descriptor = SerialDescriptor(
            type.qualifiedName!!, // TODO read @SerialName
            backend.descriptor.kind
    )

    final override fun serialize(encoder: Encoder, value: T) {
        backend.serialize(encoder, transform(value))
    }

    final override fun deserialize(decoder: Decoder): T {
        return detransform(backend.deserialize(decoder))
    }
}
