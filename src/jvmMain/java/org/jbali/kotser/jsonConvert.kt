package org.jbali.kotser

import kotlinx.serialization.json.JsonElement
import org.jbali.json.JSONArray

// TODO direct conversion
// TODO make json package under this package

fun JsonElement.convertToLegacy(): Any? =
        JSONArray("[${DefaultJson.plain.encodeToString(JsonElement.serializer(), this)}]").get(0)

fun jsonElementFromLegacy(legacyElement: Any?): JsonElement {
    val str = JSONArray(listOf(legacyElement)).toString()
    return DefaultJson.plain.decodeFromString(JsonElement.serializer(), str.substring(1, str.length - 1))
}



