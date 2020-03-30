package org.jbali.kotser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonElement

/**
 * Defines [Json] instances that are only to be used for
 * (de)serialization of [JsonElement] objects.
 */
object BasicJson {
    val plain = Json(JsonConfiguration.Stable.copy(
            prettyPrint = false
    ))
    val indented = Json(JsonConfiguration.Stable.copy(
            prettyPrint = true
    ))

    fun parse(string: String): JsonElement =
            plain.parseJson(string)

    fun stringify(element: JsonElement, prettyPrint: Boolean = true): String =
            if (prettyPrint) {
                indented
            } else {
                plain
            }.stringify(element)

}