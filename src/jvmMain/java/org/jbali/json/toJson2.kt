package org.jbali.json

import kotlinx.serialization.json.*
import org.jbali.kotser.toJsonElement
import org.jbali.kotser.unwrap


// --- legacy JSON <=> kose Json converters --- //

fun Any?.toJson2(): JsonElement =
    when (this) {
        null,
        JSONObject.NULL ->
            JsonNull
        
        is Double -> toJsonElement()
        is Boolean -> toJsonElement()
        is String -> toJsonElement()
        
        is JSONArray -> buildJsonArray {
            val a = this@toJson2
            for (i in 0 until a.length()) {
                add(a.get(i).toJson2())
            }
        }
        is JSONObject -> TODO()
        
        else -> throw IllegalArgumentException()
    }

fun JsonElement.fromJson2(): Any? =
    when (this) {
        is JsonPrimitive -> unwrap()
        
        is JsonArray ->
            map { it.fromJson2() }
                .let(::JSONArray)
        
        is JsonObject -> TODO()
    }
