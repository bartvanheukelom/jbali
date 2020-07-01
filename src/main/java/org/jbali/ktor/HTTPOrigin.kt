package org.jbali.ktor

import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import org.jbali.arrow.Uncertain
import org.jbali.arrow.nullToError


inline class HTTPOrigin(val origin: String) {
    override fun toString() = "Origin: $origin"
}

val ApplicationRequest.originHeaderOrMessage: Uncertain<HTTPOrigin>
    get() =
        header("Origin")
                .nullToError { "Request has no Origin header" }
                .map { HTTPOrigin(it) }
