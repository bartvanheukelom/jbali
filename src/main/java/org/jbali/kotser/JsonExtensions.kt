package org.jbali.kotser

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonException
import kotlinx.serialization.json.JsonObject


/**
 * Serializes [value] into an equivalent JSON using the default serializer for reified type [T].
 * @throws [JsonException] if given value can not be encoded
 * @throws [SerializationException] if given value can not be serialized
 */
@ImplicitReflectionSerializer
// extension could theoretically be attached to StringFormat instead of Json, but that's of little added value.
inline fun <reified T : Any> Json.stringify(value: T) =
        stringify(
                serializer = serializer(),
                value = value
        )

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

val JsonObject.Companion.empty get() =
    emptyJsonObject