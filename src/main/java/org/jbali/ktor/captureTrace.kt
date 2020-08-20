package org.jbali.ktor

import io.ktor.application.ApplicationCall
import io.ktor.routing.Routing
import io.ktor.routing.RoutingResolveTrace
import io.ktor.util.AttributeKey


private val akRoutingResolveTrace =
        AttributeKey<RoutingResolveTrace>("routingResolveTrace")

fun Routing.captureTrace() {
    trace {
        it.call.attributes[akRoutingResolveTrace] = it
    }
}

val ApplicationCall.capturedTrace: RoutingResolveTrace
    get() =
    attributes.getOrNull(akRoutingResolveTrace) ?: throw IllegalStateException("Trace not captured, did you call Routing.captureTrace()?")
