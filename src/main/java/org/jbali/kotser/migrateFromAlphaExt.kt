package org.jbali.kotser

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive


// ----------------  0.20.0 -> 1.0 migration helpers ------------------- //

@Deprecated("", ReplaceWith("encodeToString(serializer, value)"))
fun <T> Json.stringify(serializer: SerializationStrategy<T>, value: T) =
        encodeToString(serializer, value)

@Deprecated("", ReplaceWith("decodeFromString(deserializer, string)"))
fun <T> Json.parse(deserializer: DeserializationStrategy<T>, string: String) =
        decodeFromString(deserializer, string)

@Deprecated("", ReplaceWith("parseToJsonElement(string)"))
fun Json.parseJson(string: String) =
        parseToJsonElement(string)

@Deprecated("", ReplaceWith("encodeToString(value)", "kotlinx.serialization.encodeToString"))
inline fun <reified T> Json.stringify(value: T) =
        encodeToString(value)

@Deprecated("", ReplaceWith("decodeFromString(string)", "kotlinx.serialization.decodeFromString"))
inline fun <reified T> Json.parse(string: String): T =
        decodeFromString(string)


@Deprecated("", ReplaceWith("SetSerializer(this)", "kotlinx.serialization.builtins.SetSerializer"))
val <T> KSerializer<T>.set get() = SetSerializer(this)

@Deprecated("", ReplaceWith("ListSerializer(this)", "kotlinx.serialization.builtins.ListSerializer"))
val <T> KSerializer<T>.list get() = ListSerializer(this)

@Suppress("FunctionName")
@Deprecated("", ReplaceWith("JsonPrimitive(value)", "kotlinx.serialization.json.JsonPrimitive"))
fun JsonLiteral(value: Number?) = JsonPrimitive(value)
@Suppress("FunctionName")
@Deprecated("", ReplaceWith("JsonPrimitive(value)", "kotlinx.serialization.json.JsonPrimitive"))
fun JsonLiteral(value: Boolean?) = JsonPrimitive(value)
@Suppress("FunctionName")
@Deprecated("", ReplaceWith("JsonPrimitive(value)", "kotlinx.serialization.json.JsonPrimitive"))
fun JsonLiteral(value: String?) = JsonPrimitive(value)
