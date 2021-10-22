package org.jbali.json2

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.intellij.lang.annotations.Language
import org.jbali.kotser.BasicJson


/**
 * Contains a string whose contents are, or at least should be, valid JSON.
 * It doesn't represent a string literal like e.g. [kotlinx.serialization.json.JsonLiteral] or
 * the legacy [org.jbali.json.JSONString].
 */
@JvmInline
value class JSONString(
    @Language("JSON") val string: String
) {

    override fun toString() = string

    /**
     * Deserialize this JSON string using `kotlin-serialization`.
     * TODO make an extension in kotser package.
     */
    fun <T> parse(jsonFormat: Json, deserializer: DeserializationStrategy<T>) =
            jsonFormat.decodeFromString(deserializer, string)

    fun parse(): JsonElement =
            BasicJson.parse(string)

    fun prettify() =
            JSONString(makeJsonPretty(string))

    companion object {

        /**
         * Serialize a value to a JSON string using `kotlin-serialization`.
         * TODO make an extension in kotser package.
         */
        fun <T> stringify(jsonFormat: Json, serializer: SerializationStrategy<T>, obj: T): JSONString =
                JSONString(jsonFormat.encodeToString(serializer, obj))

        fun stringify(element: JsonElement, prettyPrint: Boolean = true): JSONString =
                JSONString(BasicJson.stringify(element, prettyPrint = prettyPrint))

    }

}

@OptIn(ExperimentalSerializationApi::class)
fun makeJsonPretty(@Language("JSON") t: String): String =
    BasicJson.indented.encodeToString(BasicJson.plain.parseToJsonElement(t))
