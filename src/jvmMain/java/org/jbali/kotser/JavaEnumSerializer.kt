package org.jbali.kotser

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Copy of [kotlinx.serialization.internal.EnumSerializer] that is not internal,
 * required because annotating Java enums with @[Serializable] is not enough.
 * TODO is it still in 1.0? if so, report as bug/feature request
 */
@OptIn(ExperimentalSerializationApi::class)
open class JavaEnumSerializer<T : Enum<T>>(
        serialName: String,
        private val values: Array<T>
) : KSerializer<T> {

    override val descriptor: SerialDescriptor =
            @OptIn(InternalSerializationApi::class)
            buildSerialDescriptor(serialName, SerialKind.ENUM) {
                values.forEach {
                    val fqn = "$serialName.${it.name}"
                    val enumMemberDescriptor = buildSerialDescriptor(fqn, StructureKind.OBJECT)
                    element(it.name, enumMemberDescriptor)
                }
            }

    override fun serialize(encoder: Encoder, value: T) {
        val index = values.indexOf(value)
        check(index != -1) {
            "$value is not a valid enum ${descriptor.serialName}, must be one of ${values.contentToString()}"
        }
        encoder.encodeEnum(descriptor, index)
    }

    override fun deserialize(decoder: Decoder): T {
        val index = decoder.decodeEnum(descriptor)
        check(index in values.indices) {
            "$index is not among valid $${descriptor.serialName} enum values, values size is ${values.size}"
        }
        return values[index]
    }
}
