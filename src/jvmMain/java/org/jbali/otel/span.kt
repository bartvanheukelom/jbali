package org.jbali.otel

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.*
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter


// span execution, blocking

fun <T> Tracer.serverSpan(
    name: String,
    parent: Context? = null,
    block: (span: Span) -> T,
): T =
    spanBuilder(name)
        .setParentOpt(parent)
        .setSpanKind(SpanKind.SERVER)
        .startAndRun(block)

fun <T> Tracer.clientSpan(
    name: String,
    block: (span: Span) -> T,
): T =
    spanBuilder(name)
        .setSpanKind(SpanKind.CLIENT)
        .startAndRun(block)

fun <T> Tracer.internalSpan(
    name: String,
    block: (span: Span) -> T,
): T =
    spanBuilder(name)
        .setSpanKind(SpanKind.INTERNAL)
        .startAndRun(block)


fun <T> SpanBuilder.startAndRun(block: (Span) -> T): T {
    val otSpan = startSpan()
    try {
        otSpan.makeCurrent().use {
            try {
                return block(otSpan)
                    .also { otSpan.setStatus(StatusCode.OK) }
            } catch (e: Throwable) {
                otSpan.setStatus(StatusCode.ERROR)
                otSpan.recordException(e)
                throw e
            }
        }
    } finally {
        otSpan.end()
    }
}


// span execution, coroutines

// TODO


// span build helpers

fun SpanBuilder.setParentOpt(parent: Context?): SpanBuilder = when (parent) {
    null -> setNoParent()
    else -> setParent(parent)
}


// context propagation

fun OpenTelemetry.propagate(injector: (key: String, value: String) -> Unit) {
    propagators.textMapPropagator.inject(Context.current(), null) { _, k, v -> injector(k, v) }
}

fun OpenTelemetry.parentContextFrom(props: Map<String, String>): Context =
    propagators.textMapPropagator.extract(Context.current(), props, object : TextMapGetter<Map<String, String>> {
        override fun keys(carrier: Map<String, String>) = carrier.keys
        override fun get(carrier: Map<String, String>?, key: String): String? = carrier?.get(key)
    })
