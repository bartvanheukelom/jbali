package org.jbali.ktor

import io.ktor.sessions.CurrentSession
import io.ktor.sessions.SessionSerializer
import io.ktor.sessions.get
import io.ktor.sessions.set
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.jbali.kotser.DefaultJson
import org.jbali.util.genericUpdate


inline fun <reified T> CurrentSession.update(
        generator: () -> T,
        updater: T.() -> T
): T =
        genericUpdate(
                getter = { get<T>() },
                generator = generator,
                updater = updater,
                setter = { set(it) }
        )

class KXSSessionSerializer<S>(
        private val serializer: KSerializer<S>,
        private val jsonFormat: Json = DefaultJson.plain
) : SessionSerializer<S> {
        override fun serialize(session: S) =
                jsonFormat.stringify(serializer, session)
        override fun deserialize(text: String) =
                jsonFormat.parse(serializer, text)
}
