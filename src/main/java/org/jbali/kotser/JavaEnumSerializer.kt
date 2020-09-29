package org.jbali.kotser

import kotlinx.serialization.*

/**
 * Copy of [kotlinx.serialization.internal.EnumSerializer] that is not internal,
 * required because annotating Java enums with @[Serializable] is not enough.
 */
open class JavaEnumSerializer<T : Enum<T>>(
        serialName: String,
        private val values: Array<T>
) : KSerializer<T> {

    override val descriptor: SerialDescriptor = SerialDescriptor(serialName, UnionKind.ENUM_KIND) {
        values.forEach {
            val fqn = "$serialName.${it.name}"
            val enumMemberDescriptor = SerialDescriptor(fqn, StructureKind.OBJECT)
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
