package org.jbali.ktor

import io.ktor.application.ApplicationCall
import io.ktor.routing.Routing
import io.ktor.routing.RoutingResolveTrace
import io.ktor.util.AttributeKey


private val akRoutingResolveTrace =
        AttributeKey<RoutingResolveTrace>("routingResolveTrace")

fun Routing.captureTrace(andAlso: (RoutingResolveTrace) -> Unit = {}) {
    trace {
        it.call.attributes[akRoutingResolveTrace] = it
        andAlso(it)
    }
}

val ApplicationCall.traceIfCaptured: RoutingResolveTrace? get() =
        attributes.getOrNull(akRoutingResolveTrace)

val ApplicationCall.capturedTrace: RoutingResolveTrace get() =
    traceIfCaptured ?: throw IllegalStateException("Trace not captured, did you call Routing.captureTrace()?")

val ApplicationCall.traceTextIfCaptured: String get() =
    traceIfCaptured?.buildText() ?: "(trace not captured)"
