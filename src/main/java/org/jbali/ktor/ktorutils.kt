package org.jbali.ktor

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.routing.PathSegmentParameterRouteSelector
import io.ktor.routing.Route
import io.ktor.util.AttributeKey
import io.ktor.util.Attributes
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.getOrFail
import io.ktor.util.pipeline.PipelineContext

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

operator fun <T : Any> Attributes.set(key: AttributeKey<T>, value: T) {
    put(key, value)
}
