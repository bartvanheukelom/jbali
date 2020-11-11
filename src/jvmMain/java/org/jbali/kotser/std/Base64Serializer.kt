package org.jbali.kotser.std

import kotlinx.serialization.KSerializer
import org.jbali.bytes.Base64Encoding
import org.jbali.bytes.Base64String
import org.jbali.kotser.StringBasedSerializer

class Base64Serializer<E : Base64Encoding<E>>(
        val encoding: Base64Encoding<E>
) : StringBasedSerializer<ByteArray>(ByteArray::class) {

    override fun fromString(s: String): ByteArray =
            encoding.decodeString(Base64String(s))

    override fun toString(o: ByteArray): String =
            encoding.encodeToString(o).encoded

}

object Base64BasicSerializer : KSerializer<ByteArray> by Base64Serializer(Base64Encoding.Basic)

