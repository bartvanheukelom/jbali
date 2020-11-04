package org.jbali.kotser

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

/**
 * Defines [Json] instances that are only to be used for
 * (de)serialization of [JsonElement] objects, as well as static utilities for doing that.
 */
object BasicJson {

    val plain = alphaStableJson
    val indented = Json(plain) {
        prettyPrint = true
    }

    fun parse(string: String): JsonElement =
            when (string) {
                "{}" -> JsonObject.empty
                "[]" -> JsonArray.empty
                "true" -> JsonPrimitive.bool(true)
                "false" -> JsonPrimitive.bool(false)
                "null" -> JsonNull
                // TODO 0 and 1?
//                "" -> TODO throw
                else -> plain.parseToJsonElement(string)
            }

    fun stringify(element: JsonElement, prettyPrint: Boolean = true): String =
            if (prettyPrint) {
                indented
            } else {
                plain
            }.stringify(element)

}

@Deprecated("", ReplaceWith("encodeToString(value)", "kotlinx.serialization.encodeToString"))
fun Json.stringify(value: JsonElement): String =
        encodeToString(value)
