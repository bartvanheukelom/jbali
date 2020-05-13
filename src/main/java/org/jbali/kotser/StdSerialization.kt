package org.jbali.kotser

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus

val stdSerializationContext = SerializersModule {
    include(dateTimeSerModule)
    include(inetAddressSerModule)
}

@UseExperimental(UnstableDefault::class)
object StdJSON {
    val plain = Json(JsonConfiguration(useArrayPolymorphism=true), stdSerializationContext)
    val indented = Json(JsonConfiguration(useArrayPolymorphism=true, prettyPrint = true), stdSerializationContext)

    // temporary until below is fixed
    fun plainWith(c: SerialModule) = Json(JsonConfiguration(useArrayPolymorphism=true), stdSerializationContext + c)
}

// TODO find out how this is properly done
fun Json.stringify(j: JsonElement): String = stringify(JsonElement.serializer(), j)
fun makeJsonPretty(t: String) = Json.indented.stringify(Json.plain.parseJson(t))

/**
 * Alternative to toJson that doesn't bug
 * TODO does it still bug in latest version?
 */
fun <T> Json.jsonify(o: T, ser: KSerializer<T>): JsonElement =
        parse(JsonElement.serializer(), stringify(ser, o))


/**
 * Nominally the same as parse, but throws more readable exceptions on parse errors.
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

// DOES NOT WORK because context + sm includes jsonModule twice
//fun Json.withContext(sm: SerialModule) =
//        Json(
//                Json::class.declaredMemberProperties.single { it.name == "configuration" }.get(this) as JsonConfiguration,
//                context + sm
//        )


/**
 * This element as [JsonLiteral] if it is one.
 * @throws [JsonElementTypeMismatchException] if it's not one.
 */
val JsonElement.literal: JsonLiteral get() =
    this as? JsonLiteral ?: throw JsonElementTypeMismatchException(this::class.toString(), "JsonLiteral")



/**
 * Union of: [Map]<String, JsonValue?> | [List]<JsonValue?> | [String] | [Boolean] | [Number]
 *
 * Since Kotlin doesn't support unions, for the compiler this is an alias of [Any].
 * TODO inline class? but should not be stored in Map/List boxed.
 * TODO number -> double
 */
typealias JsonValue = Any


/**
 * Get the content of this elements as a [JsonValue]?, or `null`.
 * I.e. for a complex structure of objects, arrays and literals, a copy of the same structure with all JSON wrappers removed.
 *
 * Assumes parsed numbers to be [Double], since while the spec does not require it, it's the only number type that is wise to use in JSON,
 * see [https://tools.ietf.org/html/rfc7159#section-6].
 *
 * [JsonLiteral]s constructed outside of parsing may contain a different [Number] type.
 */
fun JsonElement.unwrap(): JsonValue? =
        when (this) {
            is JsonObject -> this.mapValues { it.value.unwrap() }
            is JsonArray -> this.map { it.unwrap() }

            JsonNull -> null

            is JsonLiteral -> when (val b = body) {
                is Boolean, is Number -> b
                is String -> when {
                    isString -> b as String
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
                else -> throw IllegalArgumentException("'$this' has a body of unexpected type ${body.javaClass.name}")
            }
        }

/**
 * This element's [String] value if it represent a [JsonLiteral] string.
 * @throws [JsonElementTypeMismatchException] if not.
 */
val JsonElement.string: String get() =
    literal.body as? String ?: throw JsonElementTypeMismatchException(this::class.toString(), "string")

fun String.jsonElement() = JsonLiteral(this)
