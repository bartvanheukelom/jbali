package org.jbali.ktor

import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import kotlinx.serialization.Serializable
import org.jbali.arrow.Uncertain
import org.jbali.arrow.nullToError
import org.jbali.kotser.StringBasedSerializer

@Serializable(with = HTTPOrigin.Serializer::class)
data class HTTPOrigin(val origin: String) {
    override fun toString() = "Origin: $origin"
    // TODO some kind of SinglePropertySerializer
    object Serializer : StringBasedSerializer<HTTPOrigin>(HTTPOrigin::class) {
        override fun fromString(s: String) = HTTPOrigin(s)
        override fun toString(o: HTTPOrigin) = o.origin
    }
}

val ApplicationRequest.originHeaderOrMessage: Uncertain<HTTPOrigin>
    get() =
        header("Origin")
                .nullToError { "Request has no Origin header" }
                .map { HTTPOrigin(it) }
