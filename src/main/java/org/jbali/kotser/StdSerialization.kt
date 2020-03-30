package org.jbali.kotser

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus

val stdSerializationContext = SerializersModule {
    include(dateTimeSerModule)
    include(inetAddressSerModule)
}

@OptIn(UnstableDefault::class)
object StdJSON {
    val plain = Json(JsonConfiguration(useArrayPolymorphism=true), stdSerializationContext)
    val indented = Json(JsonConfiguration(useArrayPolymorphism=true, prettyPrint = true), stdSerializationContext)

    // temporary until below is fixed
    fun plainWith(c: SerialModule) = Json(JsonConfiguration(useArrayPolymorphism=true), stdSerializationContext + c)
}
