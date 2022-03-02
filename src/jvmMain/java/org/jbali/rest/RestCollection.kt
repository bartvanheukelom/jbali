package org.jbali.rest

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.util.*
import org.jbali.enums.EnumTool
import org.jbali.ktor.getExact
import org.jbali.util.ReifiedType
import org.jbali.util.cast
import org.jbali.util.reifiedTypeOf
import kotlin.reflect.KClass

fun RestRouteContext.collection(name: String, config: RestCollection.() -> Unit): RestCollection =
        RestCollection(
                context = context,
                route = ktorRouteForHacks.createRouteFromPath(name)
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
                    call.respondObject(
                        returnType = returnType,
                        returnVal = rv
                    )
                }
        }
    }

    inline fun <reified T : Any> item(
            noinline config: Item<T, String>.(key: ApplicationCall.() -> String) -> Unit
    ): Item<T, String> =
        item(
            type = reifiedTypeOf(),
            keyType = reifiedTypeOf<String>(),
            keyParse = { it },
            config = config
        )
    
    inline fun <reified E : Enum<E>, reified T : Any> enumKeyItem(
        noinline config: Item<T, E>.(key: ApplicationCall.() -> E) -> Unit
    ): Item<T, E> =
        item(
            type = reifiedTypeOf(),
            keyType = reifiedTypeOf(),
            keyParse = EnumTool<E>()::valueOf,
            config = config
        )
    
    fun <T : Any, K : Any> item(
            type: ReifiedType<T>,
            keyType: ReifiedType<K>,
            keyParse: (String) -> K,
            config: Item<T, K>.(key: ApplicationCall.() -> K) -> Unit
    ): Item<T, K> {
        
        // attempt to make key name unique (rest collections can be nested),
        // yet also readable
        // TODO support multiple of same type
        val keyParName = when (keyType) {
            reifiedTypeOf<String>() -> "key" + type.type.classifier!!.cast<KClass<*>>().simpleName!!
            else -> keyType.type.classifier!!.cast<KClass<*>>().simpleName!!
        }
        
        return Item(
            context = context,
            route = route.createChild(PathSegmentParameterRouteSelector(keyParName)),
            type = type,
            getKey = { keyParse(parameters.getOrFail(keyParName)) }
        )
            .configure {
                config(getKey)
            }
    }

    class Item<T : Any, K : Any>(
            context: RestApiContext,
            route: Route,
            type: ReifiedType<T>,
            val getKey: ApplicationCall.() -> K
    ) : RestObject<T>(
            context = context,
            route = route,
            type = type
    ) {
        val ApplicationCall.key: K get() = getKey()
        fun key(call: ApplicationCall) = call.getKey()
    }

}