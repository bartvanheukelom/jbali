package org.jbali.rest

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.routing.*
import org.jbali.ktor.getExact
import org.jbali.ktor.respondNoContent
import org.jbali.util.ReifiedType
import org.jbali.util.reifiedTypeOf

inline fun <reified T> RestRouteContext.singleton(
        name: String,
        noinline config: RestObject<T>.() -> Unit
): RestObject<T> =
        singleton(
                name = name,
                type = reifiedTypeOf(),
                config = config
        )

fun <T> RestRouteContext.singleton(
        name: String,
        type: ReifiedType<T>,
        config: RestObject<T>.() -> Unit
): RestObject<T> =
        RestObject(
                context = context,
                route = ktorRouteForHacks.createRouteFromPath(name),
                type = type
        )
            .configure(config)

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
        allowedMethods += HttpMethod.Get
        route.getExact {
            readInput(inputType)
                .let { it.impl(call) }
                .let { rv ->
                    call.respondObject(
                        returnType = type,
                        returnVal = rv
                    )
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
        allowedMethods += HttpMethod.Put
        route.put("") {
            impl(call, call.receive(inputType.type))
            call.respondNoContent()
        }
    }


    // ---------------------------- PATCH ---------------------------- //

    inline fun <reified I : Any> patch(
            noinline impl: suspend (ApplicationCall, I) -> Unit
    ) {
        patch(
                inputType = reifiedTypeOf(),
                impl = impl
        )
    }

    fun <I : Any> patch(
            inputType: ReifiedType<I>,
            impl: suspend (ApplicationCall, I) -> Unit
    ) {
        allowedMethods += HttpMethod.Patch
        route.route("", HttpMethod.Patch) {
            handle {
                impl(call, call.receive(inputType.type))
                call.respondNoContent()
            }
        }
    }
    
    
    // ---------------------------- DELETE ---------------------------- //
    
    fun delete(
        impl: suspend (ApplicationCall) -> Unit
    ) {
        allowedMethods += HttpMethod.Delete
        route.route("", HttpMethod.Delete) {
            handle {
                impl(call)
                call.respondNoContent()
            }
        }
    }
    

}