package org.jbali.json

import kotlinx.serialization.json.JsonElement
import org.jbali.kotser.StdJSON

// TODO direct conversion

fun JsonElement.convertToLegacy() =
        JSONArray("[${StdJSON.plain.stringify(JsonElement.serializer(), this)}]").get(0)

fun jsonElementFromLegacy(legacyElement: Any?): JsonElement {
    val str = JSONArray(listOf(legacyElement)).toString()
    return StdJSON.plain.parse(JsonElement.serializer(), str.substring(1, str.length-1))
}



