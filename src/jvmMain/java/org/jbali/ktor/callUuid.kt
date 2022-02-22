package org.jbali.ktor

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import java.util.*

/**
 * Install [CallId] in such a way that it tries to retrieve from the `X-Request-ID` header,
 * replies with the same header, and only accepts / generates [UUID]'s.
 */
fun ApplicationCallPipeline.installCallUuid() {
    install(CallId) {
        
        retrieveFromHeader(HttpHeaders.XRequestId)
        replyToHeader(HttpHeaders.XRequestId)
        
        generate { UUID.randomUUID().toString() }
        verify {
            try { UUID.fromString(it);              true  }
            catch (iae: IllegalArgumentException) { false }
        }
        
    }
}

/**
 * The call's UUID as provided by the `X-Request-ID` header, or generated locally
 * if not provided.
 *
 * Must have been installed with [installCallUuid].
 */
val ApplicationCall.uuid: UUID get() =
    UUID.fromString(callId!!)
