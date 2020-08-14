package org.jbali.kotser

import kotlinx.serialization.*
import kotlin.reflect.KClass
import kotlin.reflect.typeOf


/**
 * Base class for a [KSerializer] for [T] which serializes values
 * by converting them from/to [B] using the abstract methods [transform] and [detransform],
 * and delegating the actual serialization to the given [backend] serializer.
 */
abstract class TransformingSerializer<T : Any, B>(
        serialName: String,
        val backend: KSerializer<B>
) : KSerializer<T> {

    constructor(
            type: KClass<T>, // TODO read @SerialName
            backend: KSerializer<B>
    ) : this(
            serialName = type.qualifiedName!!,
            backend = backend
    )

    abstract fun transform(obj: T): B
    abstract fun detransform(tf: B): T

    override val descriptor = SerialDescriptor(
            serialName,
            backend.descriptor.kind
    )

    final override fun serialize(encoder: Encoder, value: T) {
        backend.serialize(encoder, transform(value))
    }

    final override fun deserialize(decoder: Decoder): T {
        return detransform(backend.deserialize(decoder))
    }
}

@OptIn(ImplicitReflectionSerializer::class, ExperimentalStdlibApi::class)
inline fun <reified T : Any, reified B> transformingSerializer(
        // TODO type parameters should not be included according to doc
        // TODO include the backend serialName, unless this serializer is the default for T
        serialName: String = typeOf<T>().toString(),
        crossinline transformer: (T) -> B,
        crossinline detransformer: (B) -> T
): KSerializer<T> =
        transformingSerializer(
                serialName = serialName,
                transformer = object : Transformer<T, B> {
                    override fun transform(obj: T): B = transformer(obj)
                    override fun detransform(tf: B): T = detransformer(tf)
                }
        )

interface Transformer<T, B> {
    fun transform(obj: T): B
    fun detransform(tf: B): T
}

@OptIn(ImplicitReflectionSerializer::class, ExperimentalStdlibApi::class)
inline fun <reified T : Any, reified B> transformingSerializer(
        serialName: String = typeOf<T>().toString(),
        transformer: Transformer<T, B>
): KSerializer<T> =
        object : TransformingSerializer<T, B>(
                serialName = serialName,
                backend = serializer()
        ) {
            override fun toString(): String =
                    "Serializer<${descriptor.serialName}>"
            override fun transform(obj: T): B = transformer.transform(obj)
            override fun detransform(tf: B): T = transformer.detransform(tf)
        }