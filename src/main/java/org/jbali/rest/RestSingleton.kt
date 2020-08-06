package org.jbali.rest

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.createRouteFromPath
import io.ktor.routing.get
import org.jbali.util.ClassedType
import org.jbali.util.classedTypeOf

@ExperimentalStdlibApi
inline fun <reified T : Any> RestRoute.singleton(
        name: String,
        noinline config: RestSingleton<T>.() -> Unit
): RestSingleton<T> =
        singleton(
                name = name,
                type = classedTypeOf(),
                config = config
        )

fun <T : Any> RestRoute.singleton(
        name: String,
        type: ClassedType<T>,
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
        val type: ClassedType<T>
) : RestRoute.Sub(
        context = context,
        route = route
) {

    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified I : Any> get(
            noinline impl: suspend I.(ApplicationCall) -> T
    ) {
        get(
                inputType = classedTypeOf(),
                impl = impl
        )
    }

    fun <I : Any> get(
            inputType: ClassedType<I>,
            impl: suspend I.(ApplicationCall) -> T
    ) {
        route.get("") {
            rawHandle(type) {
                readInput(inputType).impl(call)
            }
        }
    }

}