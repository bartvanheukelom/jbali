package org.jbali.ktor

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*

/**
 * Reflective access to field of Route:
 *     internal val handlers = ArrayList<PipelineInterceptor<Unit, ApplicationCall>>()
 */
private val routeFieldHandlers
    by lazy { // don't risk runtime error until needed
        Route::class.java.getDeclaredField("handlers").apply {
            isAccessible = true
        }
    }

@Suppress("UNCHECKED_CAST")
val Route.unsafeHandlers: List<PipelineInterceptor<Unit, ApplicationCall>>
    get() = routeFieldHandlers.get(this) as List<PipelineInterceptor<Unit, ApplicationCall>>

fun Route.toStringWithChildren(): String =
    buildString {
        appendLine(this@toStringWithChildren)
        children.forEach {
            append(it.toStringWithChildren().prependIndent("  "))
        }
    }

/**
 * A flat sequence of this route and all its recursive descendants.
 */
fun Route.allRoutes(): Sequence<Route> =
    sequence {
        yield(this@allRoutes)
        children.forEach {
            yieldAll(it.allRoutes())
        }
    }

data class RouteSelectorSimple(
    val path: List<String> = emptyList(), // TODO sealed class for different types of selectors (constant, param, etc)
    val method: HttpMethod? = null,
    val others: List<RouteSelector> = emptyList(),
) {
    fun plusPath(p: String) = copy(path = path + p)
}

val Route.selectorSimple: RouteSelectorSimple
    get() = (parent?.selectorSimple ?: RouteSelectorSimple()) + selector

operator fun RouteSelectorSimple.plus(s: RouteSelector): RouteSelectorSimple =
    when (s) {
        is PathSegmentParameterRouteSelector,
        is PathSegmentWildcardRouteSelector,
        is PathSegmentTailcardRouteSelector,
        is PathSegmentConstantRouteSelector,
        is PathSegmentOptionalParameterRouteSelector,
            -> plusPath(s.toString())
        
        is TrailingSlashRouteSelector
            -> copy(path = path.dropLast(1) + (path.last() + "/"))
        
        is HttpMethodRouteSelector -> copy(method = s.method)
        else -> copy(others = others + s)
    }


private val akRouteIsCatch = AttributeKey<Boolean>("isCatch")

var Route.isCatch: Boolean
    get() = attributes.contains(akRouteIsCatch)
    set(value) {
        attributes.put(akRouteIsCatch, value)
    }
