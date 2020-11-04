package org.jbali.kotser

import kotlinx.serialization.json.Json

/**
 * Container for some default instances of [Json].
 */
object DefaultJson {

    val stable = alphaStableJson

    val plain = Json(stable) {
        useArrayPolymorphism = true
        ignoreUnknownKeys = true
    }

    val read get() = plain
    val readLenient = Json(read) {
        isLenient = true
    }

    val plainOmitDefaults = Json(plain) {
        encodeDefaults = false
    }
    val indented = Json(plain) {
        prettyPrint = true
    }
    val indentedOmitDefaults = Json(indented) {
        encodeDefaults = false
    }

}
