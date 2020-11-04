package org.jbali.kotser.std

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.jbali.kotser.DefaultJson

/**
 * Serialization module with contextual serializers for some Java standard library types.
 *
 * Note that you shouldn't reach for this solution automatically whenever you need to serialize those types.
 * While cumbersome, compile-time annotating properties using those types with `@Serializable(with = ...)` is generally
 * more robust and makes the classes declaring those properties easier to serialize. Only use this if you explicitly
 * want the serialization to be dynamic based on the context.
 */
val stdSerializationContext = SerializersModule {
    include(dateTimeSerModule)
    include(inetAddressSerModule)
    include(bigNumberSerModule)
}

/**
 * Container for some [Json] instances that include [stdSerializationContext].
 * If you don't require that context, prefer using [org.jbali.kotser.DefaultJson],
 * or [org.jbali.kotser.BasicJson] if sufficient.
 */
object StdJSON {
    val plain = Json(DefaultJson.plain) {
        serializersModule = stdSerializationContext
    }
    val indented = Json(plain) {
        prettyPrint = true
    }
}
