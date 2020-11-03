package org.jbali.kotser

import kotlinx.serialization.*; import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerialModule
import kotlin.reflect.typeOf

// TODO contribute to kotlinserialization lib

fun JsonConfiguration.format(): Json =
        Json(this)

/**
 * Serializes [value] into an equivalent JSON using the default serializer for reified type [T].
 * @throws [JsonException] if given value can not be encoded
 * @throws [SerializationException] if given value can not be serialized
 */
@OptIn(ExperimentalStdlibApi::class)
@ImplicitReflectionSerializer
// extension could theoretically be attached to StringFormat instead of Json, but that's of little added value.
inline fun <reified T> Json.stringify(value: T) =
        stringify(
                serializer = try {
                    serializer<T>()
                } catch (e: Exception) {
                    throw SerializationException("Error getting static serializer for ${typeOf<T>()}: $e", e)
                },
                value = value
        )

@ImplicitReflectionSerializer
// extension could theoretically be attached to StringFormat instead of Json, but that's of little added value.
inline fun <reified T> Json.parse(string: String): T =
        parse(
                deserializer = serializer(),
                string = string
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
private val emptyJsonArray = JsonArray(emptyList())

val JsonObject.Companion.empty get() = emptyJsonObject
val JsonArray.Companion.empty get() = emptyJsonArray

val jsonTrue = JsonPrimitive(true)
val jsonFalse = JsonPrimitive(false)

fun jsonString(string: String) = JsonLiteral(string)
fun jsonString(value: Any?) = JsonLiteral(value.toString())

@Suppress("NOTHING_TO_INLINE")
inline fun JsonPrimitive.Companion.bool(b: Boolean): JsonPrimitive =
        if (b) jsonTrue else jsonFalse
