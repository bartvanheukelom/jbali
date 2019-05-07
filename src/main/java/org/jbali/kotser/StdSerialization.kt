package org.jbali.kotser

import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonElement
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

// DOES NOT WORK because context + sm includes jsonModule twice
//fun Json.withContext(sm: SerialModule) =
//        Json(
//                Json::class.declaredMemberProperties.single { it.name == "configuration" }.get(this) as JsonConfiguration,
//                context + sm
//        )
