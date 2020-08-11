package org.jbali.rest

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.routing.Route
import io.ktor.routing.createRouteFromPath
import io.ktor.routing.get
import org.jbali.util.ReifiedType
import org.jbali.util.reifiedTypeOf

inline fun <reified T : Any> RestRoute.singleton(
        name: String,
        noinline config: RestObject<T>.() -> Unit
): RestObject<T> =
        singleton(
                name = name,
                type = reifiedTypeOf(),
                config = config
        )

fun <T : Any> RestRoute.singleton(
        name: String,
        type: ReifiedType<T>,
        config: RestObject<T>.() -> Unit
): RestObject<T> =
        RestObject(
                context = context,
                route = route.createRouteFromPath(name),
                type = type
        ).apply(config)

open class RestObject<T : Any>(
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

//    fun <T : Any> put(
//            type: ReifiedType<T>,
//            impl: suspend PipelineContext<Unit, A>(ApplicationCall) -> T
//    ) {
//        get(inputType = ReifiedType.unit) {
//            impl(it)
//        }
//    }

}