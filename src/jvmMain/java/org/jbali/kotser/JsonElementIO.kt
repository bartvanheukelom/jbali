package org.jbali.kotser

import kotlinx.serialization.json.*
import org.jbali.json2.JsonableList
import org.jbali.json2.JsonableLiteral
import org.jbali.json2.JsonableMap
import org.jbali.json2.JsonableValue


// ====== reading from JsonElement ====== //


// TODO report bug in JsonPrimitive doc, about content returning `null` instead of "null"

/**
 * Get the content of this element as a [JsonableValue]?, or `null`.
 * I.e. for a complex structure of objects, arrays and literals, a copy of the same structure with all [JsonElement] wrappers removed.
 *
 * Assumes parsed numbers to be [Double], since while the spec does not require it, it's the only number type that is wise to use in JSON,
 * see [https://tools.ietf.org/html/rfc7159#section-6].
 *
 * [JsonLiteral]s constructed outside of parsing may contain a different [Number] type.
 */
fun JsonElement.unwrap(): JsonableValue? =
        when (this) {
            is JsonObject -> this.mapValues { it.value.unwrap() }
            is JsonArray -> this.map { it.unwrap() }
            is JsonPrimitive -> unwrap()
        }

/**
 * Get the content of a [JsonPrimitive].
 * @throws IllegalArgumentException if the literal is invalid.
 */
fun JsonPrimitive.unwrap(): JsonableLiteral? =
        when {

            this is JsonNull -> null

            isString -> content

            content.equals("true", ignoreCase = true) -> true
            content.equals("false", ignoreCase = true) -> false

            doubleOrNull != null -> double

            else -> throw IllegalArgumentException("'$this' appears to be an invalid JSON literal")

        }

// TODO contribute to lib
/**
 * This element's [String] value if it's a [JsonLiteral] that represent a string.
 * @throws [IllegalArgumentException] if not.
 */
val JsonElement.string: String get() =
    jsonPrimitive.takeIf { it.isString }?.content
        ?: throw IllegalArgumentException("${this::class} is not a string")



// ====== constructing JsonElement ====== //

fun String.toJsonElement() = JsonPrimitive(this)
fun Double.toJsonElement() = JsonPrimitive(this)
fun Boolean.toJsonElement() = JsonPrimitive(this)

fun JsonableMap.toJsonElement() =
        buildJsonObject {
            entries.forEach { (k, v) ->
                k to v.toJsonElement()
            }
        }

fun JsonableList.toJsonElement() =
        toJsonArray()

/**
 * Wrap this value in the appropriate [JsonElement] container.
 * Deeply, for [Map] and [List].
 * @throws IllegalArgumentException if this is not a valid [JsonableValue].
 */
// TODO since this is just Any, this extension is at high risk of being imported accidentally. rename all these functions
fun JsonableValue?.toJsonElement(): JsonElement =
        when (this) {
            null -> JsonNull

            is Double -> toJsonElement()
            is Boolean -> toJsonElement()
            is String -> toJsonElement()

            is Map<*, *> ->
                buildJsonObject {
                    entries.forEach { (k, v) ->
                        val sk = k as? String
                                ?: throw IllegalArgumentException("$this is not a valid JsonableMap, it has non-string key: $k")
                        sk to v.toJsonElement()
                    }
                }

            is List<*> ->
                // IntelliJ doesn't require this cast, but compiler does (though it shouldn't?)
                (this as Iterable<*>).toJsonArray {
                    add(it.toJsonElement())
                }

            else -> throw IllegalArgumentException("$this is not a valid JsonableValue")
        }

/**
 * Map this iterable to a [JsonArray].
 *
 * Unlike [map], the lambda should not return the value to be added, but should call
 * the proper overload of [JsonArrayBuilder.unaryPlus].
 *
 * The default [adder] simply maps each item using [toJsonElement].
 */
inline fun <T> Iterable<T>.toJsonArray(
        crossinline adder: JsonArrayBuilder.(T) -> Unit = { add(it.toJsonElement()) }
): JsonArray =
        buildJsonArray {
            forEach {
                adder(it)
            }
        }
