package org.jbali.kotser

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import org.jbali.util.PartialObject
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

class PartialObjectSerializer<T : Any>(
        tSerializer: KSerializer<T>
) : KSerializer<PartialObject<T>> {

    private val type =
            tSerializer.javaClass.kotlin
                    .memberFunctions.single {
                        it.name == "deserialize"
                    }
                    .returnType

    override val descriptor =
            @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
            buildSerialDescriptor(
                    serialName = "${PartialObject::class.qualifiedName!!}<$type>",
                    kind = StructureKind.MAP,
                    tSerializer.descriptor
            ) {
                element<String>("0")
                // TODO no idea if this is in any way correct
                element("1", buildSerialDescriptor("Any?", PolymorphicKind.OPEN))
            }

    override fun serialize(encoder: Encoder, value: PartialObject<T>) {
        with (value.properties) {
            encoder.beginCollection(descriptor, size).apply {
                forEachIndexed { i, e ->
                    encodeSerializableElement(descriptor, i*2, String.serializer(), e.name)
                    // TODO e.findAnnotation<Serializable>()!!.with
                    encodeSerializableElement(descriptor, i*2+1, serializer(e.returnType), value.getValue(e))
                }
                endStructure(descriptor)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder) =
            PartialObject.Mapped(buildMap<KProperty1<T, *>, Any?> {
                decoder.beginStructure(descriptor).apply {
                    if (decodeSequentially()) {
                        TODO()
                    } else {
                        while (true) {
                            val index = decodeElementIndex(descriptor)
                            if (index == CompositeDecoder.DECODE_DONE) break

                            val propName = decodeSerializableElement(descriptor, index, String.serializer())

                            @Suppress("UNCHECKED_CAST")
                            val prop: KProperty1<T, Any?> =
                                    (type.classifier as KClass<T>)
                                            .memberProperties
                                            .single { it.name == propName }

                            val vIndex = decodeElementIndex(descriptor).also {
                                require(it == index + 1) { "Value must follow key in a map, index for key: $index, returned index for value: $it" }
                            }
                            val value = decodeSerializableElement(descriptor, vIndex, serializer(prop.returnType))

                            put(prop, value)
                        }
                    }
                    endStructure(descriptor)
                }
            })

}