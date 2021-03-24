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

operator fun <T : Any> Attributes.set(key: AttributeKey<T>, value: T) {
    put(key, value)
}
