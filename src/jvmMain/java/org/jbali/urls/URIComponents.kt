@file:JvmName("URIComponents")

package org.jbali.urls

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@JvmName("encode")
fun encodeURIComponent(str: String): String =
    try {
        URLEncoder.encode(str, StandardCharsets.UTF_8.name())
    } catch (e: UnsupportedEncodingException) {
        throw AssertionError(e)
    }

@JvmName("decode")
fun decodeURIComponent(str: String): String =
    try {
        URLDecoder.decode(str, StandardCharsets.UTF_8.name())
    } catch (e: UnsupportedEncodingException) {
        throw AssertionError(e)
    }
