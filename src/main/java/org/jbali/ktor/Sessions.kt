package org.jbali.ktor

import io.ktor.sessions.CurrentSession
import io.ktor.sessions.get
import io.ktor.sessions.set
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