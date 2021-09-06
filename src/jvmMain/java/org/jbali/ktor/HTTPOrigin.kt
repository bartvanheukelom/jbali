package org.jbali.ktor

import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import kotlinx.serialization.Serializable
import org.jbali.arrow.Uncertain
import org.jbali.arrow.nullToError
import org.jbali.kotser.StringBasedSerializer

/**
 * Represents an Origin as used in CORS, i.e. a protocol/scheme, host and port.
 *
 * [https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Origin]
 *
 * Coincidentally, the exact properties required to establish a connection to a server. This class may also be used as such.
 * TODO rename it to something more appropriate then, and don't use Url for storage
 *
 * @throws IllegalArgumentException if [url] includes unused components.
 */
@Serializable(with = HTTPOrigin.Serializer::class)
data class HTTPOrigin(
        /**
         * The [Url] representation of this origin. Only the protocol, host and port parts may be used.
         * The port must not be specified if it's equal to the default port of the protocol.
         */
        val url: Url
) {

    constructor(string: String) : this(Url(string).withoutDefaultPort())

    val scheme: URLProtocol get() = url.protocol
    val hostname: String get() = url.host
    val port: Int? get() = url.portIfSpecified

    val string: String = url.toString()

    init {
        with (url) {
            require(encodedPath.isEmpty()) {
                "Url($url).encodedPath not empty: $encodedPath"
            }
            require(parameters.isEmpty())
            require(fragment.isEmpty())
            require(user == null)
            require(password == null)
            require(!trailingQuery)
            require(!specifiesDefaultPort) {
                "Url($url) must not specify default port"
            }
        }
    }

    override fun toString() = "Origin: $url"

    object Serializer : StringBasedSerializer<HTTPOrigin>(HTTPOrigin::class) {
        override fun fromString(s: String) = HTTPOrigin(s)
        override fun toString(o: HTTPOrigin) = o.string
    }
}


val RequestConnectionPoint.httpOrigin: HTTPOrigin get() =
    this.let { c ->
        HTTPOrigin(URLBuilder().apply {
            protocol = URLProtocol.createOrDefault(c.scheme)
                .wsToHttp()
            host = c.host
            if (c.port != protocol.defaultPort) {
                port = c.port
            }
            encodedPath = ""
        }.build())
    }


val ApplicationRequest.originHeaderOrMessage: Uncertain<HTTPOrigin>
    get() = originHeaderIfGiven.nullToError { "Request has no Origin header" }

val ApplicationRequest.originHeaderIfGiven: HTTPOrigin?
    get() = header("Origin")
                ?.let { HTTPOrigin(Url(it)) }

/**
 * Get the [HTTPOrigin] given by the `Origin` request header, or if that header is not present
 * (e.g. if the request is a simple browser GET), deduce the origin from the [origin] [RequestConnectionPoint].
 */
val ApplicationRequest.deducedOrigin: HTTPOrigin
    get() = originHeaderIfGiven ?: origin.httpOrigin

/**
 * Returns whether these URLs point to the same [HTTPOrigin].
 * Note that this returns `false` for e.g. an "https" URL and a "wss" URL that point to the same host and port.
 * Use [Url.sameOrigin] when you need to consider those as equal.
 */
infix fun Url.sameOrigin(other: Url): Boolean =
    protocol == other.protocol && sameOrigin(other)

val Url.origin get() =
    HTTPOrigin(Url(
        protocol = protocol,
        host = host,
        specifiedPort = portOrDefault,

        encodedPath = "",
        parameters = Parameters.Empty,
        fragment = "",
        user = null,
        password = null,
        trailingQuery = false,
    ))
