package org.jbali.ktor

import io.ktor.features.origin
import io.ktor.http.RequestConnectionPoint
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import kotlinx.serialization.Serializable
import org.jbali.arrow.Uncertain
import org.jbali.arrow.nullToError
import org.jbali.kotser.StringBasedSerializer

/**
 * Represents an Origin as used in CORS.
 *
 * [https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Origin]
 *
 * @throws IllegalArgumentException if [url] includes unused components.
 */
@Serializable(with = HTTPOrigin.Serializer::class)
data class HTTPOrigin(
        /**
         * The [Url] representation of this origin. Only the protocol, host and port parts may be used.
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
