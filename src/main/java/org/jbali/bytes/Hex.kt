package org.jbali.bytes

import org.jbali.util.HexBytes

/**
 * Represents one of the 3 standard Base64 encoding schemes.
 */
sealed class Hex<E : Hex<E>>(private val upperCase: Boolean) : StringEncoding<ByteArray, E> {

    override fun encodeToString(value: ByteArray): StringEncoded<ByteArray, E> =
            StringEncoded(HexBytes.toHex(value, value.size, upperCase))

    override fun decodeString(encoded: StringEncoded<ByteArray, E>): ByteArray =
            HexBytes.parseHex(encoded.string)

    object Lower : Hex<Lower>(false)
    object Upper : Hex<Upper>(true)

}
