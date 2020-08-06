package org.jbali.kotser

import kotlinx.serialization.*
import kotlin.reflect.KProperty1

class SinglePropertySerializer<T : Any, P>(
        serialName: String,
        private val fromPropertyConstructor: (P) -> T,
        private val propertyGetter: T.() -> P,
        private val propertySerializer: KSerializer<P>
) : KSerializer<T> {

    override val descriptor = SerialDescriptor(
            serialName,
            propertySerializer.descriptor.kind
    )

    override fun toString() =
            "Serializer<${descriptor.serialName}>"

    final override fun serialize(encoder: Encoder, value: T) {
        value.propertyGetter()
                .let { propertySerializer.serialize(encoder, it) }
    }

    final override fun deserialize(decoder: Decoder): T =
        propertySerializer.deserialize(decoder)
                .let(fromPropertyConstructor)

}

@OptIn(ExperimentalStdlibApi::class, ImplicitReflectionSerializer::class)
inline fun <reified T : Any, reified P> singlePropertySerializer(
        prop: KProperty1<T, P>,
        // TODO can this be generated, either at compile time, or at runtime using information from reflection?
        crossinline wrap: (P) -> T
): KSerializer<T> =
        transformingSerializer(
                transformer = { prop.get(it) },
                detransformer = wrap
        )