package org.jbali.bytes

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import org.jbali.kotser.Transformer
import org.jbali.kotser.transformingSerializer
import org.jbali.util.toHexString

/**
 * Simple container for a byte array that gives it:
 * - value semantics for equals/hashCode
 * - toString() that outputs hex
 * - default serialization as Base64
 * - TODO make immutable
 * - TODO implement List<Byte>
 */
@Serializable(with = BinaryData.Serializer::class)
class BinaryData(
        // var on purpose, making it val would give a false sense of immutability
        var data: ByteArray
) {

    override fun toString(): String =
            "BinaryData(${data.toHexString()})"

    override fun equals(other: Any?): Boolean =
            when {
                other === this -> true
                other is BinaryData -> data.contentEquals(other.data)
                else -> false
            }

    // TODO if this were immutable, this could be cached
    override fun hashCode(): Int =
            data.contentHashCode()
    
    companion object {
        /** Conveniently create a [BinaryData] from a Base64 basic string, e.g. in unit tests. */
        fun fromBase64(b: String) = Base64Transformer.detransform(b)
    }

    object Serializer : KSerializer<BinaryData> by transformingSerializer(Base64Transformer)

    object Base64Transformer : Transformer<BinaryData, String> {

        override fun transform(obj: BinaryData): String =
                Base64Encoding.Basic.encodeToString(obj.data).string

        override fun detransform(tf: String): BinaryData = tf
                .let(::Base64BasicString)
                .decode()
                .asData()

    }

}

fun ByteArray.asData() = BinaryData(this)
