package org.jbali.json2

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import org.jbali.kotser.BasicJson
import org.jbali.kotser.stringify


/**
 * Contains a string whose contents are, or at least should be, valid JSON.
 * It doesn't represent a string literal like e.g. [kotlinx.serialization.json.JsonLiteral] or
 * the legacy [org.jbali.json.JSONString].
 */
inline class JSONString(
        val string: String
) {

    override fun toString() = string

    /**
     * Deserialize this JSON string using `kotlin-serialization`.
     * TODO make an extension in kotser package.
     */
    fun <T> parse(jsonFormat: Json, deserializer: DeserializationStrategy<T>) =
            jsonFormat.parse(deserializer, string)

    fun prettify() =
            JSONString(makeJsonPretty(string))

    companion object {

        /**
         * Serialize a value to a JSON string using `kotlin-serialization`.
         * TODO make an extension in kotser package.
         */
        fun <T> stringify(jsonFormat: Json, serializer: SerializationStrategy<T>, obj: T) =
                jsonFormat.stringify(serializer, obj)

    }

}

fun makeJsonPretty(t: String) =
        BasicJson.indented.stringify(BasicJson.plain.parseJson(t))
