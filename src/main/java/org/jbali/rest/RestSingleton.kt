package org.jbali.rest

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.createRouteFromPath
import io.ktor.routing.get
import org.jbali.util.ReifiedType
import org.jbali.util.reifiedTypeOf

@ExperimentalStdlibApi
inline fun <reified T : Any> RestRoute.singleton(
        name: String,
        noinline config: RestSingleton<T>.() -> Unit
): RestSingleton<T> =
        singleton(
                name = name,
                type = reifiedTypeOf(),
                config = config
        )

fun <T : Any> RestRoute.singleton(
        name: String,
        type: ReifiedType<T>,
        config: RestSingleton<T>.() -> Unit
): RestSingleton<T> =
        RestSingleton(
                context = context,
                route = route.createRouteFromPath(name),
                type = type
        ).apply(config)

class RestSingleton<T : Any>(
        context: RestApiContext,
        route: Route,
        val type: ReifiedType<T>
) : RestRoute.Sub(
        context = context,
        route = route
) {

    inline fun <reified I : Any> get(
            noinline impl: suspend I.(ApplicationCall) -> T
    ) {
        get(
                inputType = reifiedTypeOf(),
                impl = impl
        )
    }

    fun <I : Any> get(
            inputType: ReifiedType<I>,
            impl: suspend I.(ApplicationCall) -> T
    ) {
        route.get("") {
            rawHandle(type) {
                readInput(inputType).impl(call)
            }
        }
    }

}