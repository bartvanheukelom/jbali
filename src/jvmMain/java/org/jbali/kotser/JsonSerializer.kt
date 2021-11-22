package org.jbali.kotser

import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.intellij.lang.annotations.Language
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
    
    fun elementTransformer() =
        object : Transformer<T, JsonElement> {
            override fun transform(obj: T) = encodeToElement(obj)
            override fun detransform(tf: JsonElement) = decodeFromElement(tf)
        }
    
    fun stringTransformer() =
        object : Transformer<T, JSONString> {
            override fun transform(obj: T) = stringify(obj)
            override fun detransform(tf: JSONString) = parse(tf)
        }

    // convenience methods:

    fun parseJsonString(@Language("JSON") str: String) = parse(JSONString(str))
    
    fun rawStringTransformer() =
        object : Transformer<T, String> {
            override fun transform(obj: T) = stringify(obj).string
            override fun detransform(@Language("JSON") tf: String) = parse(JSONString(tf))
        }

}

fun <T> T.stringifyWith(s: JsonSerializer<T>): JSONString =
        s.stringify(this)

fun <T> JSONString.parseWith(s: JsonSerializer<T>): T =
        s.parse(this)

inline fun <reified T> JSONString.decode(): T =
    string.decodeJson()
inline fun <reified T> String.decodeJson(): T =
    DefaultJson.read.decodeFromString(this)



// --- convenience factories --- //

// not sure why this exists
fun <T> jsonSerializer(
        serializer: KSerializer<T>,
        format: Json = DefaultJson.plainOmitDefaults
) = JsonSerializer(
        serializer = serializer,
        format = format
)

// known type
inline fun <reified T> jsonSerializer(
    format: Json = DefaultJson.plainOmitDefaults
) = JsonSerializer<T>(
    serializer = kotlinx.serialization.serializer(),
    format = format
)

// KSerializer converter
@JvmName("jsonSerializerExt")
fun <T> KSerializer<T>.jsonSerializer(
    format: Json = DefaultJson.plainOmitDefaults
) = JsonSerializer(this, format)
