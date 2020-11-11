package org.jbali.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HMAC(
        val algorithm: String = "HmacSHA256",
        val key: ByteArray
) {

    private val k = SecretKeySpec(key, algorithm)

    fun sign(msg: ByteArray): ByteArray =
            Mac.getInstance(algorithm).run {
                init(k)
                doFinal(msg)
            }

}