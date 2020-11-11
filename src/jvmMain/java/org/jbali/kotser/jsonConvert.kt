package org.jbali.kotser

import kotlinx.serialization.json.JsonElement
import org.jbali.json.JSONArray

// TODO direct conversion
// TODO make json package under this package

fun JsonElement.convertToLegacy() =
        JSONArray("[${DefaultJson.plain.stringify(JsonElement.serializer(), this)}]").get(0)

fun jsonElementFromLegacy(legacyElement: Any?): JsonElement {
    val str = JSONArray(listOf(legacyElement)).toString()
    return DefaultJson.plain.parse(JsonElement.serializer(), str.substring(1, str.length-1))
}



