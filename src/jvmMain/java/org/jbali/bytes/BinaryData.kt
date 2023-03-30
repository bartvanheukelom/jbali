package org.jbali.bytes

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import org.jbali.kotser.Transformer
import org.jbali.kotser.transformingSerializer
import org.jbali.math.toIntExact
import org.jbali.util.toHexString
import java.nio.charset.Charset
import kotlin.random.Random

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
            "BinaryData(${toHexString()})"

    fun toHexString() =
        data.toHexString()
    
    fun toBase64() =
        Base64Encoding.Basic.encodeToString(data).string
    
    val size get() = data.size.toULong()

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
        
        @JvmField val EMPTY = BinaryData(ByteArray(0))
        
        /**
         * Conveniently create a [BinaryData] from a Base64 string.
         * Whitespace is removed from the input, which must otherwise conform to the Basic Base64 decoding rules.
         * TODO support URL-safe too.
         */
        fun fromBase64(b: String): BinaryData =
                b.asSequence()
                        .filterNot { it == '\n' || it == '\r' || it.isWhitespace() }
                        .joinToString("")
                        .let(Base64Transformer::detransform)
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
fun String.toData(charset: Charset = Charsets.UTF_8) = toByteArray(charset).asData()

fun Random.nextData(size: ULong): BinaryData = nextBytes(size.toIntExact()).asData()
fun Random.nextData(size: Int):   BinaryData = nextBytes(size             ).asData()
