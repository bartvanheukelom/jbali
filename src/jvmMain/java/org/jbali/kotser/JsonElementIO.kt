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
// numbers that convert to Double without loss (however, type info is lost)
fun Byte.toJsonElement() = JsonPrimitive(toDouble())
fun Short.toJsonElement() = JsonPrimitive(toDouble())
fun Int.toJsonElement() = JsonPrimitive(toDouble())
// TODO make name consistent when https://youtrack.jetbrains.com/issue/KT-35305 fixed
fun UByte.toJsonElementU() = JsonPrimitive(toDouble())
fun UShort.toJsonElementU() = JsonPrimitive(toDouble())
fun UInt.toJsonElementU() = JsonPrimitive(toDouble())

fun JsonableMap.toJsonElement() =
        buildJsonObject {
            entries.forEach { (k, v) ->
                k to v.toJsonElement()
            }
        }

fun JsonableList.toJsonElement() =
        mapToJsonArray()

@Deprecated("Useless JsonElement.toJsonElement(). Did you expect the receiver to be something else?", ReplaceWith("this"))
fun JsonElement.toJsonElement(): JsonElement = this

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
    
            // numbers that convert to Double without loss
            is Byte -> toJsonElement()
            is Short -> toJsonElement()
            is Int -> toJsonElement()
            is UByte -> toJsonElementU()
            is UShort -> toJsonElementU()
            is UInt -> toJsonElementU()

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
                (this as Iterable<*>).mapToJsonArray()

            else -> throw IllegalArgumentException("$this is not a valid JsonableValue")
        }

/**
 * Map this iterable to a [JsonArray].
 * The default [mapper] simply maps each item using [toJsonElement].
 */
inline fun <T> Iterable<T>.mapToJsonArray(
        crossinline mapper: (T) -> JsonElement = { it.toJsonElement() }
): JsonArray =
        buildJsonArray {
            forEach {
                add(mapper(it))
            }
        }


// ========= JsonObjectBuilder extensions ===========

fun JsonObjectBuilder.put(key: String, value: String)  = put(key, value.toJsonElement())
fun JsonObjectBuilder.put(key: String, value: Double)  = put(key, value.toJsonElement())
fun JsonObjectBuilder.put(key: String, value: Boolean) = put(key, value.toJsonElement())
fun JsonObjectBuilder.put(key: String, value: Byte) = put(key, value.toJsonElement())
fun JsonObjectBuilder.put(key: String, value: Short) = put(key, value.toJsonElement())
fun JsonObjectBuilder.put(key: String, value: Int) = put(key, value.toJsonElement())
fun JsonObjectBuilder.put(key: String, value: UByte) = put(key, value.toJsonElementU())
fun JsonObjectBuilder.put(key: String, value: UShort) = put(key, value.toJsonElementU())
fun JsonObjectBuilder.put(key: String, value: UInt) = put(key, value.toJsonElementU())

fun JsonObjectBuilder.putStrings (key: String, value: Iterable<String >) = put(key, value.mapToJsonArray { it.toJsonElement() })
fun JsonObjectBuilder.putDoubles (key: String, value: Iterable<Double >) = put(key, value.mapToJsonArray { it.toJsonElement() })
fun JsonObjectBuilder.putBooleans(key: String, value: Iterable<Boolean>) = put(key, value.mapToJsonArray { it.toJsonElement() })


// for https://youtrack.jetbrains.com/issue/KT-35305
//fun Any?.foo() {}
//fun Int.foo() {}
//fun UInt.foo() {}
//@JvmInline value class UInt24(val data: UInt) : Comparable<UInt24> {
//    override fun compareTo(other: UInt24): Int = T ODO()
//
//}
//fun UInt24.foo() {}
//
//fun test() {
//    1.foo()  // resolves to Int.foo()
//    1u.foo() // Overload resolution ambiguity
//    UInt24(1u).foo() // resolves to UInt24.foo()
//}
