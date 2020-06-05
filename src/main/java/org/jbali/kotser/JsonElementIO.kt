package org.jbali.kotser

import kotlinx.serialization.json.*
import org.jbali.json2.JsonableList
import org.jbali.json2.JsonableLiteral
import org.jbali.json2.JsonableMap
import org.jbali.json2.JsonableValue
import org.jbali.math.toDoubleExact


// ====== reading from JsonElement ====== //

/**
 * This element as [JsonLiteral] if it is one.
 * @throws [JsonException] if it's not one.
 */
val JsonElement.literal: JsonLiteral
    get() =
        this as? JsonLiteral ?: throw JsonException("${this::class} is not a JsonLiteral")


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
            is JsonLiteral -> unwrap()
            is JsonNull -> null
        }

/**
 * Get the contents of a [JsonLiteral], which takes care of dealing with its awkward API.
 * @throws IllegalArgumentException if the literal is invalid.
 * @throws ArithmeticException if the literal contains a number not representable as double.
 */
fun JsonLiteral.unwrap(): JsonableLiteral =
        when (val b = body) {

            is Boolean,
            is Double -> b

            is String -> when {
                isString -> b
                else -> when {
                    // from StringOps.kt, String.toBooleanStrictOrNull
                    b.equals("true", ignoreCase = true) -> true
                    b.equals("false", ignoreCase = true) -> false

                    // body can be any string, but number is the only legal JSON type left
                    else ->
                        try {
                            b.toDouble()
                        } catch (e: NumberFormatException) {
                            IllegalArgumentException("'$this' appears to be an invalid JSON literal")
                        }
                }
            }

            is Number -> b.toDoubleExact()

            else -> throw IllegalArgumentException("'$this' has a body of unexpected type ${body.javaClass.name}")
        }

/**
 * This element's [String] value if it's a [JsonLiteral] that represent a string.
 * @throws [JsonException] if not.
 */
val JsonElement.string: String get() =
    literal.body as? String ?: throw JsonException("${this::class} is not a string")



// ====== constructing JsonElement ====== //

fun String.toJsonElement() = JsonLiteral(this)
fun Double.toJsonElement() = JsonLiteral(this)
fun Boolean.toJsonElement() = JsonLiteral(this)

fun JsonableMap.toJsonElement() =
        json {
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
fun JsonableValue?.toJsonElement(): JsonElement =
        when (this) {
            null -> JsonNull

            is Double -> toJsonElement()
            is Boolean -> toJsonElement()
            is String -> toJsonElement()

            is Map<*, *> ->
                json {
                    entries.forEach { (k, v) ->
                        val sk = k as? String
                                ?: throw IllegalArgumentException("$this is not a valid JsonableMap, it has non-string key: $k")
                        sk to v.toJsonElement()
                    }
                }

            is List<*> ->
                // IntelliJ doesn't require this cast, but compiler does (though it shouldn't?)
                (this as Iterable<*>).toJsonArray {
                    +it.toJsonElement()
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
        crossinline adder: JsonArrayBuilder.(T) -> Unit = { +it.toJsonElement() }
): JsonArray =
        jsonArray {
            forEach {
                adder(it)
            }
        }
