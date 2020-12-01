package org.jbali.kotser

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for values of [Any] type. Like [ContextualSerializer][kotlinx.serialization.ContextualSerializer],
 * delegates the actual serialization to a serializer retrieved from the context. However, where ContextualSerializer
 * calls [getContextual][kotlinx.serialization.modules.SerializersModule.getContextual] with the runtime class of the value,
 * [ContextualAnySerializer] calls it with `Any::class`, and leaves any runtime deductions to the returned contextual serializer.
 *
 * Simply said, this serializer piggybacks on the serialization context mechanism to delegate to another serializer at runtime,
 * and nothing more.
 *
 * Does not support deserialization.
 */
@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
object ContextualAnySerializer : KSerializer<Any> {

    override val descriptor: SerialDescriptor =
            buildSerialDescriptor("org.jbali.kotser.ContextualAnySerializer", SerialKind.CONTEXTUAL)

    override fun serialize(encoder: Encoder, value: Any) {
        val serializer = encoder.serializersModule.getContextual(Any::class) ?:
            throw SerializationException("ContextualAnySerializer cannot work in the current context, which has no serializer defined for Any.")
        encoder.encodeSerializableValue(serializer, value)
    }

    override fun deserialize(decoder: Decoder): Any {
        throw UnsupportedOperationException("ContextualAnySerializer is write-only")
    }
}
