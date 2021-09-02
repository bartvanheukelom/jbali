package org.jbali.kotser

import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jbali.json2.JSONString

/**
 * Utility that binds together a [KSerializer] for [T] and a [Json] format to use.
 */
data class JsonSerializer<T>(
        val serializer: KSerializer<T>,
        val format: Json
) {

    fun encodeToElement(obj: T): JsonElement = format.encodeToJsonElement(serializer, obj)
    fun decodeFromElement(element: JsonElement): T = format.decodeFromJsonElement(serializer, element)

    fun stringify(obj: T): JSONString = JSONString.stringify(format, serializer, obj)
    fun parse(str: JSONString) = str.parse(format, serializer)

    // convenience methods:

    fun parseJsonString(str: String) = parse(JSONString(str))

}

fun <T> T.stringifyWith(s: JsonSerializer<T>): JSONString =
        s.stringify(this)

fun <T> JSONString.parseWith(s: JsonSerializer<T>): T =
        s.parse(this)

inline fun <reified T> JSONString.decode(): T =
    string.decodeJson()
inline fun <reified T> String.decodeJson(): T =
    DefaultJson.read.decodeFromString(this)

inline fun <reified T> jsonSerializer(
        serializer: KSerializer<T> = kotlinx.serialization.serializer(),
        format: Json = DefaultJson.plain
) = JsonSerializer(
        serializer = serializer,
        format = format
)
