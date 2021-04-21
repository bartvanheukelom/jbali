package org.jbali.bytes

import java.util.*
import kotlin.jvm.JvmInline

/**
 * Represents one of the 3 standard Base64 encoding schemes.
 */
sealed class Base64Encoding<E : Base64Encoding<E>>(
        val encoder: Base64.Encoder,
        val decoder: Base64.Decoder
) : StringEncoding<ByteArray, E> {

//    fun encodeToString(src: ByteArray): Base64String<E> =
//            Base64String<E>(encoder.encodeToString(src))

    override fun encodeToString(value: ByteArray): StringEncoded<ByteArray, E> =
            StringEncoded(encoder.encodeToString(value))

//    fun decode(enc: Base64String<E>): ByteArray =
//            decoder.decode(enc.encoded)

    override fun decodeString(encoded: StringEncoded<ByteArray, E>): ByteArray =
            decoder.decode(encoded.string)

    object Basic : Base64Encoding<Basic>(
            encoder = Base64.getEncoder(),
            decoder = Base64.getDecoder()
    )

    object Mime : Base64Encoding<Mime>(
            encoder = Base64.getMimeEncoder(),
            decoder = Base64.getMimeDecoder()
    )

    object Url : Base64Encoding<Url>(
            encoder = Base64.getUrlEncoder(),
            decoder = Base64.getUrlDecoder()
    )

}

//@JvmInline
//value class Base64String<E : Base64Encoding<E>>(
//        val encoded: String
//) {
//    override fun toString() = encoded
//}

interface StringEncoding<V, E : StringEncoding<V, E>> {
    fun encodeToString(value: V): StringEncoded<V, E>
    fun decodeString(encoded: StringEncoded<V, E>): V
}

infix fun <V, E : StringEncoding<V, E>> V.encodedAs(e: StringEncoding<V, E>): StringEncoded<V, E> =
        e.encodeToString(this)

// TODO when serialization supports inline class, serialize this as the contents
@JvmInline
value class StringEncoded<V, E : StringEncoding<V, E>>(
        private val v: String
) {
    @get:Deprecated("string", ReplaceWith("string"))
    val encoded get() = v
    val string get() = v

    override fun toString() = string
}

typealias Base64String<E> = StringEncoded<ByteArray, E>
typealias Base64BasicString = Base64String<Base64Encoding.Basic>
// TODO the other 2

fun Base64BasicString.decode(): ByteArray = Base64Encoding.Basic.decodeString(this)
// TODO the other 2 (or can this be done with inline tricks?)

/**
 * Container for a URL-safe Base64 string that
 * encodes a UTF-8 encoded string, which is wrappable in, or convertible to,
 * a value of type [S].
 */
@JvmInline
value class Base64Utf8String<E : Base64Encoding<E>, S>(
        val encoded: Base64String<E>
) {

    override fun toString() = encoded.toString()

    @OptIn(ExperimentalStdlibApi::class)
    fun decode(encoding: E, wrapper: String.() -> S): S =
            encoded
                    .let(encoding::decodeString)
                    .decodeToString()
                    .wrapper()

    companion object {
        fun <E : Base64Encoding<E>, S> encode(v: S, unwrapper: S.() -> String, encoding: E): Base64Utf8String<E, S> =
                v
                        .unwrapper()
                        .toByteArray()
                        .let(encoding::encodeToString)
                        .let { Base64Utf8String(it) }
    }
}

