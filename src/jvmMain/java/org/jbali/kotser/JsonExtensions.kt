package org.jbali.kotser

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
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
        decodeFromString(JsonElement.serializer(), encodeToString(ser, o))


/**
 * Nominally the same as [Json.parse], but throws more readable exceptions on parse errors.
 * TODO the default exceptions are better now, check if this function is still of value
 */
fun <T> Json.parseDiag(deserializer: DeserializationStrategy<T>, string: String): T =
        try {
            try {
                decodeFromString(deserializer, string)
            } catch (e: Throwable) {
                // see if the error is caused by JSON syntax (throws if it is)
                parseToJsonElement(string)
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

fun <T : JsonElement> List<T>    .toJsonArray() = JsonArray(this         )
fun <T : JsonElement> Iterable<T>.toJsonArray() = JsonArray(this.toList())
fun <T : JsonElement> Sequence<T>.toJsonArray() = JsonArray(this.toList())

fun <T : JsonElement> Iterable<Pair<String, T>>.toJsonObject() = JsonObject(toMap())
fun <T : JsonElement> Sequence<Pair<String, T>>.toJsonObject() = JsonObject(toMap())

@Suppress("NOTHING_TO_INLINE")
inline fun JsonPrimitive.Companion.bool(b: Boolean): JsonPrimitive =
        if (b) jsonTrue else jsonFalse


fun JsonObjectBuilder.putExclusive(key: String, value: JsonElement) {
    val prev = put(key, value)
    if (prev != null) {
        put(key, prev)
        throw IllegalArgumentException("Key '$key' already present in JsonObjectBuilder")
    }
}

fun JsonObjectBuilder.putAllExclusive(source: Map<String, JsonElement>) {
    source.forEach { (k, v) -> putExclusive(k, v) }
}

infix fun JsonObject.plusExclusive(other: JsonObject) =
    buildJsonObject {
        putAllExclusive(this@plusExclusive)
        putAllExclusive(other)
    }
