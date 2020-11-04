package org.jbali.ktor

import io.ktor.sessions.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.jbali.kotser.DefaultJson
import org.jbali.util.GeneratorUpdater
import org.jbali.util.createOrUpdate

inline fun <reified T : Any> CurrentSession.createOrUpdate(
        gup: GeneratorUpdater<T>
): T =
        createOrUpdate(
                getter = { get<T>() },
                gup = gup,
                setter = { set(it) }
        )

/**
 * KTOR [SessionSerializer] implementation using kotlinx.serialization.
 *
 * KXS = KotlinX.Serialization
 */
class KXSSessionSerializer<S : Any>(
        private val serializer: KSerializer<S>,
        private val jsonFormat: Json = DefaultJson.plain
) : SessionSerializer<S> {
        override fun serialize(session: S) =
                jsonFormat.encodeToString(serializer, session)
        override fun deserialize(text: String) =
                jsonFormat.decodeFromString(serializer, text)
}
