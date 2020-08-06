package org.jbali.rest

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.createRouteFromPath
import io.ktor.routing.get
import org.jbali.util.ClassedType
import org.jbali.util.classedTypeOf

fun RestRoute.collection(name: String, config: RestCollection.() -> Unit): RestCollection =
        RestCollection(
                context = context,
                route = route.createRouteFromPath(name)
        ).apply(config)

class RestCollection(
        context: RestApiContext,
        route: Route
) : RestRoute.Sub(
        context = context,
        route = route
) {

    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified I : Any, reified T : Any> index(
            noinline impl: suspend I.(ApplicationCall) -> T
    ) {
        index(
                inputType = classedTypeOf(),
                returnType = classedTypeOf(),
                impl = impl
        )
    }

    fun <I : Any, T : Any> index(
            inputType: ClassedType<I>,
            returnType: ClassedType<T>,
            impl: suspend I.(ApplicationCall) -> T
    ) {
        route.get("") {
            rawHandle(returnType) {
                readInput(type = inputType).impl(call)
            }
        }
    }

}