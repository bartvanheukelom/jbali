package org.jbali.kotser

import kotlinx.serialization.*
import kotlinx.serialization.json.*



/**
 * Json instance with configuration equivalent to the old `JsonConfiguration.Stable` in pre 1.0 versions of kotlinx-serialization.
 */
val alphaStableJson = Json {
    allowStructuredMapKeys = true
}



/**
 * Alternative to toJson that doesn't bug
 * TODO does it still bug in latest version?
 */
fun <T> Json.jsonify(o: T, ser: KSerializer<T>): JsonElement =
        parse(JsonElement.serializer(), stringify(ser, o))


/**
 * Nominally the same as [Json.parse], but throws more readable exceptions on parse errors.
 * TODO the default exceptions are better now, check if this function is still of value
 */
fun <T> Json.parseDiag(deserializer: DeserializationStrategy<T>, string: String): T =
        try {
            try {
                parse(deserializer, string)
            } catch (e: Throwable) {
                // see if the error is caused by JSON syntax (throws if it is)
                parseJson(string)
                // it's not, so throw the original exception
                throw e
            }
        } catch (e: Throwable) {
            throw IllegalArgumentException("While deserializing with $deserializer from JSON:\n${string.prependIndent("  ")}\ngot error: $e", e)
        }

private val emptyJsonObject = JsonObject(emptyMap())
private val emptyJsonArray = JsonArray(emptyList())

val JsonObject.Companion.empty get() = emptyJsonObject
val JsonArray.Companion.empty get() = emptyJsonArray

val jsonTrue = JsonPrimitive(true)
val jsonFalse = JsonPrimitive(false)

fun jsonString(string: String) = JsonPrimitive(string)
fun jsonString(value: Any?) = JsonPrimitive(value.toString())

@Suppress("NOTHING_TO_INLINE")
inline fun JsonPrimitive.Companion.bool(b: Boolean): JsonPrimitive =
        if (b) jsonTrue else jsonFalse
