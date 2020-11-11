package org.jbali.ktor

import io.ktor.http.DEFAULT_PORT
import io.ktor.http.Url


val Url.portIfSpecified: Int? get() =
    specifiedPort.takeUnless { it == DEFAULT_PORT }

val Url.specifiesDefaultPort: Boolean get() =
    specifiedPort == protocol.defaultPort

/**
 * Return a copy of this [Url] with the port unspecified,
 * if it's specified to the protocol's default port.
 */
fun Url.withoutDefaultPort(): Url =
        if (specifiedPort != protocol.defaultPort) {
            this
        } else {
            copy(specifiedPort = DEFAULT_PORT)
        }
