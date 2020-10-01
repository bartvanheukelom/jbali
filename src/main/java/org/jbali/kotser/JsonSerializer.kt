package org.jbali.kotser

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.jbali.json2.JSONString

/**
 * Utility that binds together a [KSerializer] for [T] and a [Json] format to use.
 */
data class JsonSerializer<T>(
        val serializer: KSerializer<T>,
        val format: Json
) {

    fun stringify(obj: T): JSONString = JSONString.stringify(format, serializer, obj)
    fun parse(str: JSONString) = str.parse(format, serializer)

    // convenience methods:

    fun parseJsonString(str: String) = parse(JSONString(str))

}

fun <T> T.stringifyWith(s: JsonSerializer<T>): JSONString =
        s.stringify(this)

fun <T> JSONString.parseWith(s: JsonSerializer<T>): T =
        s.parse(this)

@OptIn(ImplicitReflectionSerializer::class)
inline fun <reified T> jsonSerializer(
        serializer: KSerializer<T> = kotlinx.serialization.serializer(),
        format: Json = DefaultJson.plain
) = JsonSerializer(
        serializer = serializer,
        format = format
)
