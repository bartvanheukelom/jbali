package org.jbali.ktor

import io.ktor.http.*


val Url.portIfSpecified: Int? get() =
    specifiedPort.takeUnless { it == DEFAULT_PORT }

val Url.specifiesDefaultPort: Boolean get() =
    specifiedPort == protocol.defaultPort

@Deprecated("", ReplaceWith("port"))
val Url.portOrDefault: Int get() = port

/**
 * Return whether these URLs point to the same host + port.
 */
infix fun Url.sameEndpoint(other: Url) =
    host == other.host && port == other.port
    

/**
 * Return a copy of this [Url] with the port unspecified,
 * if it's specified to the protocol's default port.
 *
 * For example:
 * - "https://wikipedia.org:443" -> "https://wikipedia.org"
 * - "http://localhost:8080" => "http://localhost:8080"
 */
fun Url.withoutDefaultPort(): Url =
        if (specifiedPort != protocol.defaultPort) {
            this
        } else {
            copy(specifiedPort = DEFAULT_PORT)
        }

/**
 * For protocols having [isSecure], returns the insecure version.
 * E.g. for [URLProtocol.HTTPS] returns [URLProtocol.HTTP].
 * For other, including those insecure versions themselves, returns `null`.
 */
fun URLProtocol.withoutSecure(): URLProtocol? =
    when (this) {
        URLProtocol.HTTPS -> URLProtocol.HTTP
        URLProtocol.WSS -> URLProtocol.WS
        else -> null
    }

/**
 * For protocols not having [isSecure], returns the secure version if one exists.
 * E.g. for [URLProtocol.HTTP] returns [URLProtocol.HTTPS].
 * For those that already have [isSecure], or for which no secure version exists, returns `null`.
 */
fun URLProtocol.withSecure(): URLProtocol? =
    when (this) {
        URLProtocol.HTTP -> URLProtocol.HTTPS
        URLProtocol.WS -> URLProtocol.WSS
        else -> null
    }

/**
 * For websocket protocols, returns the equivalent HTTP protocol.
 * For others, returns `null`.
 */
fun URLProtocol.wsHttpEquivalent(): URLProtocol? =
    when (this) {
        URLProtocol.WSS -> URLProtocol.HTTPS
        URLProtocol.WS -> URLProtocol.HTTP
        else -> null
    }

/**
 * For websocket protocols, returns the equivalent HTTP protocol.
 * For others, returns `this`.
 */
fun URLProtocol.wsToHttp(): URLProtocol =
    wsHttpEquivalent() ?: this
