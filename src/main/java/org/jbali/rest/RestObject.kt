package org.jbali.rest

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.routing.Route
import io.ktor.routing.createRouteFromPath
import io.ktor.routing.get
import io.ktor.routing.put
import org.jbali.ktor.respondNoContent
import org.jbali.util.ReifiedType
import org.jbali.util.reifiedTypeOf

inline fun <reified T> RestRoute.singleton(
        name: String,
        noinline config: RestObject<T>.() -> Unit
): RestObject<T> =
        singleton(
                name = name,
                type = reifiedTypeOf(),
                config = config
        )

fun <T> RestRoute.singleton(
        name: String,
        type: ReifiedType<T>,
        config: RestObject<T>.() -> Unit
): RestObject<T> =
        RestObject(
                context = context,
                route = route.createRouteFromPath(name),
                type = type
        ).apply(config)

open class RestObject<T>(
        context: RestApiContext,
        route: Route,
        val type: ReifiedType<T>
) : RestRoute.Sub(
        context = context,
        route = route
) {

    // ---------------------------- GET ---------------------------- //

    inline fun <reified I : Any> get(
            noinline impl: suspend I.(ApplicationCall) -> T
    ) {
        get(
                inputType = reifiedTypeOf(),
                impl = impl
        )
    }

    fun get(
            impl: suspend (ApplicationCall) -> T
    ) {
        get(inputType = ReifiedType.unit) {
            impl(it)
        }
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


    // ---------------------------- PUT ---------------------------- //

    inline fun <reified I : Any> put(
            noinline impl: suspend (ApplicationCall, I) -> Unit
    ) {
        put(
                inputType = reifiedTypeOf(),
                impl = impl
        )
    }

    fun <I : Any> put(
            inputType: ReifiedType<I>,
            impl: suspend (ApplicationCall, I) -> Unit
    ) {
        route.put("") {
            rawHandle(ReifiedType.unit) {
                impl(call, call.receive(inputType.type))
            }
            call.respondNoContent()
        }
    }

}