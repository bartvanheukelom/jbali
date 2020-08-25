package org.jbali.kotser

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import org.jbali.util.PartialObject
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties

class PartialObjectSerializer<T : Any>(val type: KType) : KSerializer<PartialObject<T>> {

    constructor(tSerializer: KSerializer<T>) : this(
            tSerializer.javaClass.kotlin
                    .memberFunctions.single { it.name == "deserialize" }
                    .returnType
    )

    @OptIn(ImplicitReflectionSerializer::class)
    override val descriptor = SerialDescriptor(
            serialName = PartialObject::class.qualifiedName!!,
            kind = StructureKind.MAP
    ) {
        element<String>("0")
        element("1", SerialDescriptor("Any?", PolymorphicKind.OPEN))
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

    @OptIn(ExperimentalStdlibApi::class)
    override fun deserialize(decoder: Decoder) =
            PartialObject.Mapped(buildMap<KProperty1<T, *>, Any?> {
                decoder.beginStructure(descriptor).apply {
                    if (decodeSequentially()) {
                        TODO()
                    } else {
                        while (true) {
                            val index = decodeElementIndex(descriptor)
                            if (index == CompositeDecoder.READ_DONE) break

                            val propName = decodeSerializableElement(descriptor, index, String.serializer())

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