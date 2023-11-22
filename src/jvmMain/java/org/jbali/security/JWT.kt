package org.jbali.security

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jbali.bytes.*
import org.jbali.json2.JSONString
import org.jbali.kotser.*
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.Signature
import java.security.spec.RSAPublicKeySpec
import java.time.Instant


@Serializable
data class JWTHeader(
    val typ: String? = null,
    val alg: String,
    val kid: String? = null,
)


typealias Base64JSON = Base64Utf8String<Base64Encoding.Url, JSONString>


@JvmInline
value class JWTSignature(
    val encoded: Base64UrlString,
) {
    override fun toString() = encoded.toString()
}

@Serializable(with = JWT.Serializer::class)
data class JWT(
    val header: Base64JSON,
    val payload: Base64JSON,
    val signature: JWTSignature,
) {
    override fun toString() = "$header.$payload.$signature"
    val concat get() = toString()

    companion object {
        @JvmStatic
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
    
    object Serializer : KSerializer<JWT> by transformingSerializer(
        transformer = { it.concat },
        detransformer = { fromString(it) },
    )

}

// TODO move
fun Base64JSON.unbase64(): JSONString =
        decode(encoding = Base64Encoding.Url, wrapper = { JSONString(this) })

private fun JSONString.base64(): Base64JSON =
        Base64JSON.encode(v = this, unwrapper = { string }, encoding = Base64Encoding.Url)

private fun Base64Utf8String<Base64Encoding.Url, BigInteger>.unbase64(): BigInteger =
    encoded.decode().let { BigInteger(1, it) }

abstract class JWTSigner(
    val alg: String
) {

    fun sign(header: Base64JSON, payload: Base64JSON): JWTSignature =
        signData(signatureInput(header, payload))
            .let(Base64Encoding.Url::encodeToString)
            .let { JWTSignature(it) }
    
    open fun verify(token: JWT) {
        
        val h = token.header.unbase64().decode<JWTHeader>()
        
        require(h.alg == alg) {
            "JWT header alg invalid: $h"
        }
    
        // check signature
        val goodSig = sign(token.header, token.payload)
        require(goodSig == token.signature) {
            // TODO log the required signature? (but don't send to client!)
            "JWT signature different"
        }
    }

    protected abstract fun signData(d: ByteArray): ByteArray
    
    protected fun signatureInput(
        header: Base64JSON,
        payload: Base64JSON
    ): ByteArray =
        "$header.$payload".toByteArray()

    
    
    abstract class HmacBased(bits: Int, key: ByteArray) : JWTSigner("HS$bits") {
        private val hmac = HMAC(algorithm = "HmacSHA$bits", key = key)

        override fun signData(d: ByteArray): ByteArray =
                hmac.sign(d)
    }

    class HS256(key: ByteArray) : HmacBased(256, key)
    class HS384(key: ByteArray) : HmacBased(384, key)
    class HS512(key: ByteArray) : HmacBased(512, key)
    
    
    class RSA(bits: Int, private val keys: KeyPair) : JWTSigner("RS$bits") {
        init {
            require(bits in setOf(256, 384, 512))
            require(keys.public != null || keys.private != null) {
                "KeyPair must contain at least a public or private key"
            }
        }
    
        override fun signData(d: ByteArray) =
            TODO("Not yet implemented")
    
        override fun verify(token: JWT) {
            check(keys.public != null)
    
            val h = token.header.unbase64().decode<JWTHeader>()
    
            require(h.alg == alg) {
                "JWT header alg invalid: $h"
            }
            
            require(Signature.getInstance("SHA256withRSA")
                .apply {
                    initVerify(keys.public)
                    update(signatureInput(token.header, token.payload))
                }.verify(token.signature.encoded.decode())) {
                "JWT signature invalid"
            }
            
        }
    }
    
    
    class KeySet(keySet: JWKS) : JWTSigner("KeySet") {
        
        private val signers: Map<String, JWTSigner> =
            keySet.keys.associate {
                it.kid!! to using(it)
            }
        
        override fun signData(d: ByteArray) =
            error("JWTSigner.KeySet can't sign, only verify")
    
        override fun verify(token: JWT) {
            val h = token.header.unbase64().decode<JWTHeader>()
            val kid = h.kid ?: throw IllegalArgumentException("kid missing from JWT header")
            val signer = signers[kid] ?: throw IllegalArgumentException("Key with kid $kid is unknown to us")
            signer.verify(token)
        }
    }
    
    companion object {
        fun using(key: JWK): JWTSigner =
            when {
                key.alg == null ->
                    throw IllegalArgumentException("Cannot create JWTSigner for key without alg")
                key.alg.startsWith("RS") -> {
                    val bits = key.alg.substring(2).toInt()
                    RSA(bits, KeyPair(
                        KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(
                            key.n!!.unbase64(),
                            key.e!!.unbase64(),
                        )),
                        null, // TODO
                    ))
                }
                else -> throw IllegalArgumentException("Cannot create JWTSigner for alg ${key.alg}")
            }
    }
}

/**
 * Serializer for [Instant] according to the JWT `NumericDate` specification.
 */
object JWTNumericDateSerializer : TransformingSerializer<Instant, Double>(Instant::class, Double.serializer()) {
    override fun transform(obj: Instant) =
            obj.epochSecond.toDouble()

    override fun detransform(tf: Double): Instant =
            Instant.ofEpochSecond(tf.toLong())
}

inline fun <reified P : Any> JWTManager(
    json: Json = DefaultJson.plain,
    signer: JWTSigner,
) = JWTManager<P>(serializer(), json, signer)

class JWTManager<P : Any>(
        val payloadSerializer: KSerializer<P>,
        val json: Json = DefaultJson.plain,
        val signer: JWTSigner
) {

    fun issue(payload: P): JWT {
        val h = JWTHeader(
            typ = "JWT",
            alg = signer.alg
        )

        val hs = h.stringifyWith(jsonSerializer()).base64()
        val ps = JSONString(json.encodeToString(payloadSerializer, payload)).base64()

        val sig = signer.sign(hs, ps)

        return JWT(hs, ps, sig)
    }

    /**
     * @throws IllegalArgumentException if the token is not a valid JWT, uses a different signing algorithm or is not signed correctly.
     */
    fun verify(token: JWT) {

        // read and check header
        val h = token.header.unbase64().decode<JWTHeader>()
        require((h.typ ?: "JWT") == "JWT") {
            "JWT header typ invalid: $h"
        }
    
        signer.verify(token)

    }

    fun verifyAndParse(token: JWT): P {
        verify(token)
        return token.payload.unbase64().parse(json, payloadSerializer)
    }

}

@Serializable
data class JWKS(
    val keys: List<JWK>,
)

/**
 * https://datatracker.ietf.org/doc/html/rfc7517
 */
@Serializable
data class JWK(
    val kty: String,
    val use: String? = null,
    val alg: String? = null,
    val kid: String? = null,
    // TODO should not need to do @Contextual
    // TODO it's the wrong type, the conversion is base64 -> bytes -> bigint (no strings involved)
    val n: Base64Utf8String<@Contextual Base64Encoding.Url, @Contextual BigInteger>? = null,
    val e: Base64Utf8String<@Contextual Base64Encoding.Url, @Contextual BigInteger>? = null,
)
