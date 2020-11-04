package org.jbali.kotser

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import org.jbali.json2.JSONString
import org.jbali.json2.KVON

/**
 * Parse [KVON] to objects of type [T].
 * @param jsonFormat The [Json] to use for parsing. Will be modified to have [kotlinx.serialization.json.JsonBuilder.isLenient] `true`, since
 *                   [KVON.toJson] (having no knowledge of [T]) cannot determine whether an input of `"true"`
 *                   is supposed to be a boolean or string, and will output it as a quoted string.
 *                   A lenient [Json] allows deserializing such a string to a [Boolean].
 */
class KVONDeserializer<T : Any>(
        val deserializer: DeserializationStrategy<T>,
        jsonFormat: Json = DefaultJson.readLenient,
        private val intermediateJsonCallback: (JSONString) -> Unit = {}
) {

    private val jsonFormat = Json(jsonFormat) {
        isLenient = true
    }

    fun deserialize(kvon: KVON): T =
            kvon.asJsonObject
                    .also(intermediateJsonCallback)
                    .parse(jsonFormat, deserializer)

}
