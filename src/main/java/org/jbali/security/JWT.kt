package org.jbali.security



import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jbali.bytes.Base64Encoding
import org.jbali.bytes.Base64String
import org.jbali.bytes.Base64Utf8String
import org.jbali.json2.JSONString
import org.jbali.kotser.DefaultJson
import org.jbali.kotser.TransformingSerializer
import java.time.Instant


data class JWTHeader(
        val typ: String,
        val alg: String
)


typealias Base64JSON = Base64Utf8String<Base64Encoding.Url, JSONString>


inline class JWTSignature(
        val encoded: Base64String<Base64Encoding.Url>
) {
    override fun toString() = encoded.toString()
}

data class JWT(
        val header: Base64JSON,
        val payload: Base64JSON,
        val signature: JWTSignature
) {
    override fun toString() = "$header.$payload.$signature"

    companion object {
        fun fromString(s: String): JWT {
            val split = s.split('.')
            require(split.size == 3) {
                "JWT string invalid: $s"
            }
            return JWT(
                    // TODO check here if input is valid base64
                    Base64JSON(Base64String(split[0])),
                    Base64JSON(Base64String(split[1])),
                    JWTSignature(Base64String(split[2]))
            )
        }
    }

}

private fun Base64JSON.unbase64(): JSONString =
        decode(encoding = Base64Encoding.Url, wrapper = { JSONString(this) })

private fun JSONString.base64(): Base64JSON =
        Base64JSON.encode(v = this, unwrapper = { string }, encoding = Base64Encoding.Url)

abstract class JWTSigner(
        val alg: String
) {

    fun sign(header: Base64JSON, payload: Base64JSON): JWTSignature =
            signData("$header.$payload".toByteArray())
                    .let(Base64Encoding.Url::encodeToString)
                    .let { JWTSignature(it) }

    protected abstract fun signData(d: ByteArray): ByteArray

    abstract class HmacBased(bits: Int, key: ByteArray) : JWTSigner("HS$bits") {
        private val hmac = HMAC(algorithm = "HmacSHA$bits", key = key)

        override fun signData(d: ByteArray): ByteArray =
                hmac.sign(d)
    }

    class HS256(key: ByteArray) : HmacBased(256, key)
    class HS384(key: ByteArray) : HmacBased(384, key)
    class HS512(key: ByteArray) : HmacBased(512, key)
}

/**
 * Serializer for [Instant] according to the JWT `NumericDate` specification.
 */
object JWTNumericDateSerializer : TransformingSerializer<Instant, Double>(Double.serializer()) {
    override fun transform(obj: Instant) =
            obj.epochSecond.toDouble()

    override fun detransform(tf: Double): Instant =
            Instant.ofEpochSecond(tf.toLong())
}

class JWTManager<P : Any>(
        val payloadSerializer: KSerializer<P>,
        val json: Json = DefaultJson.plain,
        val signer: JWTSigner
) {

    @Serializable
    data class Header(
            val typ: String,
            val alg: String
    )

    fun issue(payload: P): JWT {
        val h = Header(
                typ = "JWT",
                alg = signer.alg
        )

        val hs = JSONString(json.stringify(Header.serializer(), h)).base64()
        val ps = JSONString(json.stringify(payloadSerializer, payload)).base64()

        val sig = signer.sign(hs, ps)

        return JWT(hs, ps, sig)
    }

    /**
     * @throws IllegalArgumentException if the token is not a valid JWT, uses a different signing algorithm or is not signed correctly.
     */
    fun verify(token: JWT) {

        // read and check header
        val h = token.header.unbase64().parse(json, Header.serializer())
        require(h.typ == "JWT") {
            "JWT header typ invalid: $h"
        }
        require(h.alg == signer.alg) {
            "JWT header alg invalid: $h"
        }

        // check signature
        val goodSig = signer.sign(token.header, token.payload)
        require(goodSig == token.signature) {
            // TODO log the required signature? (but don't send to client!)
            "JWT signature different"
        }

    }

    fun verifyAndParse(token: JWT): P {
        verify(token)
        return token.payload.unbase64().parse(json, payloadSerializer)
    }

}
