package org.jbali.rest

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.serialization.json.buildJsonObject
import org.jbali.enums.EnumTool
import org.jbali.kotser.put
import org.jbali.ktor.getExact
import org.jbali.oas.Operation
import org.jbali.oas.PathItem
import org.jbali.oas.Response
import org.jbali.util.ReifiedType
import org.jbali.util.cast
import org.jbali.util.reifiedTypeOf
import kotlin.reflect.KClass

fun RestRouteContext.collection(
    name: String,
    config: RestCollection.() -> Unit,
): RestCollection =
        RestCollection(
            context = context,
            route = ktorRouteForHacks.createRouteFromPath(name),
            parent = restRoute,
            subPath = name,
        )
            .configure(config)

class RestCollection(
        context: RestApiContext,
        route: Route,
        parent: RestRoute? = null,
        subPath: String? = null,
) : RestRoute.Sub(
    context = context,
    route = route,
    parent = parent,
    subPath = subPath,
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
        oasPath("", PathItem(
            get = Operation(
                responses = mapOf(
                    HttpStatusCode.OK.value.toString() to Response(
                        description = "$returnType",
                        content = buildJsonObject {
                            put(ContentType.Application.Json.contentType, buildJsonObject {
                                put("schema", buildJsonObject {
                                    put("type", "object")
                                    put("properties",buildJsonObject {
                                        put("TODO",buildJsonObject {
                                            put("type", "string")
                                        })
                                    })
                                })
                            })
                        }
                    )
                )
            )
        ))
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
    
    
    inline fun <reified T : Any, reified K : Any> item(
        noinline keyParse: (String) -> K,
        noinline config: Item<T, K>.(key: ApplicationCall.() -> K) -> Unit
    ): Item<T, K> =
        item(
            type = reifiedTypeOf(),
            keyType = reifiedTypeOf(),
            keyParse = keyParse,
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