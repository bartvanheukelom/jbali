package org.jbali.ktor

import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*

/**
 * Create a child [Route] where the first path segment is captured as a parameter, named [name].
 */
@OptIn(KtorExperimentalAPI::class)
fun Route.pathParameter(
    name: String,
    build: Route.(PipelineContext<Unit, ApplicationCall>.() -> String) -> Unit
): Route {
    @Suppress("UnnecessaryVariable")
    val paramName = name // TODO default to lambda param name
    return createChild(PathSegmentParameterRouteSelector(paramName)).apply {
        val getParameterValue: PipelineContext<Unit, ApplicationCall>.() -> String = {
            call.parameters.getOrFail(paramName)
        }
        build(getParameterValue)
    }
}

fun Route.toStringWithChildren(): String =
    buildString {
        appendLine(this@toStringWithChildren)
        children.forEach {
            append(it.toStringWithChildren().prependIndent("  "))
        }
    }

/**
 * Builds a route to match specified [path] and defines a handler for it.
 */
@ContextDsl
fun Route.handlePath(path: String, body: PipelineInterceptor<Unit, ApplicationCall>): Route {
    return route(path) { handle(body) }
}

/**
 * Builds 2 routes, to match the paths "" and "/" respectively, and invokes [build] on both.
 * TODO why does "/" work? is this some kind of legacy behaviour? document this. also document what this route doesn't match.
 */
fun Route.routeExact(build: Route.() -> Unit) {
    route("", build)
    route("/", build)
}

/**
 * Calls [routeExact], installing a handler using [body].
 */
@ContextDsl
fun Route.handleExact(body: PipelineInterceptor<Unit, ApplicationCall>) {
    routeExact { handle(body) }
}

/**
 * Calls [routeExact], installing a handler for [get] using [body].
 */
@ContextDsl
fun Route.getExact(body: PipelineInterceptor<Unit, ApplicationCall>) {
    routeExact { get(body) }
}

/**
 * Builds a route to match any path and defines a handler for it.
 */
@ContextDsl
fun Route.handleAnyPath(body: PipelineInterceptor<Unit, ApplicationCall>): Route {
    return route("{...}") { handle(body) }
}

/**
 * [RouteSelector] that always returns a match with low quality.
 * Probably serves no purpose...
 */
object FallbackRouteSelector :
    RouteSelector(0.05) {

    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int) =
        RouteSelectorEvaluation.Constant

    override fun toString(): String = "(fallback)"
}
