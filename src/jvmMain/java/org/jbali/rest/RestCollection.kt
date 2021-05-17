package org.jbali.rest

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.util.*
import org.jbali.ktor.getExact
import org.jbali.util.ReifiedType
import org.jbali.util.reifiedTypeOf

fun RestRoute.collection(name: String, config: RestCollection.() -> Unit): RestCollection =
        RestCollection(
                context = context,
                route = route.createRouteFromPath(name)
        )
            .configure(config)

class RestCollection(
        context: RestApiContext,
        route: Route
) : RestRoute.Sub(
        context = context,
        route = route
) {

    inline fun <reified I : Any, reified T : Any> index(
            noinline impl: suspend I.(ApplicationCall) -> T
    ) {
        index(
                inputType = reifiedTypeOf(),
                returnType = reifiedTypeOf(),
                impl = impl
        )
    }

    inline fun <reified T : Any> index(
            noinline impl: suspend (ApplicationCall) -> T
    ) {
        index(
                inputType = ReifiedType.unit,
                returnType = reifiedTypeOf()
        ) {
            impl(it)
        }
    }

    inline fun <reified T : Any> index(
            noinline impl: suspend () -> T
    ) {
        index(
                inputType = ReifiedType.unit,
                returnType = reifiedTypeOf()
        ) {
            impl()
        }
    }

    fun <I : Any, T : Any> index(
            inputType: ReifiedType<I>,
            returnType: ReifiedType<T>,
            impl: suspend I.(ApplicationCall) -> T
    ) {
        allowedMethods += HttpMethod.Get
        route.getExact {
            readInput(inputType)
                .let { it.impl(call) }
                .let { rv ->
                    respondObject(
                        returnType = returnType,
                        returnVal = rv
                    )
                }
        }
    }

    inline fun <reified T : Any> item(
            noinline config: Item<T>.() -> Unit
    ): Item<T> =
            item(
                    type = reifiedTypeOf(),
                    config = config
            )

    fun <T : Any> item(
            type: ReifiedType<T>,
            config: Item<T>.() -> Unit
    ): Item<T> =
            @OptIn(KtorExperimentalAPI::class)
            Item(
                    context = context,
                    route = route.createChild(PathSegmentParameterRouteSelector("key")),
                    type = type,
                    getKey = { parameters.getOrFail("key") }
            )
                .configure(config)

    class Item<T : Any>(
            context: RestApiContext,
            route: Route,
            type: ReifiedType<T>,
            val getKey: ApplicationCall.() -> String
    ) : RestObject<T>(
            context = context,
            route = route,
            type = type
    ) {
        val ApplicationCall.key: String get() = getKey()
    }

}