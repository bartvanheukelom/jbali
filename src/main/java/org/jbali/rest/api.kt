package org.jbali.rest

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.content.TextContent
import io.ktor.response.ApplicationResponse
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.createRouteFromPath
import io.ktor.routing.get
import io.ktor.serialization.serialization
import io.ktor.util.flattenEntries
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jbali.json2.KVON
import org.jbali.kotser.DefaultJson
import org.jbali.kotser.KVONDeserializer
import org.jbali.kotser.serializer
import org.jbali.util.ClassedType
import org.jbali.util.classedTypeOf
import kotlin.reflect.KType


fun Route.restApi(
        jsonFormat: Json = DefaultJson.indented,

        config: RestApiBuilder.() -> Unit
) =
        RestApiBuilder(
                route = this,
                jsonFormat = jsonFormat
        ).also { it.config() }


data class RestApiBuilder(
        val route: Route,

        val jsonFormat: Json
) {

    init {
        // TODO now that this exists, can return objects directly?
        route.install(ContentNegotiation) {
            serialization(
                    contentType = ContentType.Application.Json,
                    format = jsonFormat
            )
        }
    }

    inner class Collection(
            // when Collection was an inner class, accessing jsonFormat from an inline function compiled,
            // but threw IllegalAccessError.
//            val api: RestApiBuilder,
            val colRoute: Route
    ) {

        @OptIn(ExperimentalStdlibApi::class)
        inline fun <reified I : Any, reified T : Any> index(noinline impl: I.(ApplicationCall) -> T) {
            index(
                    inputType = classedTypeOf(),
                    returnType = classedTypeOf(),
                    impl = impl
            )
        }

        fun <I : Any, T : Any> index(inputType: ClassedType<I>, returnType: ClassedType<T>, impl: I.(ApplicationCall) -> T) {
            colRoute.get("") {

                val returnJson =
                        try {

                            // TODO error handling

                            // compose input
                            // TODO on error, give HttpStatusCode.BadRequest
                            val input: I = readInput(
                                    type = inputType,
                                    jsonFormat = jsonFormat
                            )

                            // call implementation
                            val returnVal = input.impl(call)

                            // serialize return value
                            // TODO on error, HttpStatusCode.InternalServerError
                            jsonFormat.stringify(serializer(returnType.type), returnVal)

                        } catch (e: Throwable) {
                            // TODO remove
                            System.err.println("---------------------------------------------------------------------------")
                            e.printStackTrace()
                            System.err.println("---------------------------------------------------------------------------")
                            Thread.sleep(100)
                            throw e
                        }

                // instruct client how to deserialize the response
                call.response.addContentTypeInnerHeader(returnType.type)

                // respond JSON response
                call.respond(TextContent(
                        text = returnJson,
                        contentType = ContentType.Application.Json
                ))
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        suspend inline fun <reified T : Any> PipelineContext<Unit, ApplicationCall>.rawHandle(
                noinline impl: suspend () -> T
        ) {
            rawHandle(
                    returnType = classedTypeOf(),
                    impl = impl
            )
        }

        suspend fun <T : Any> PipelineContext<Unit, ApplicationCall>.rawHandle(
                returnType: ClassedType<T>,
                impl: suspend () -> T
        ) {
            val returnJson =
                    try {

                        // TODO error handling

                        // call implementation
                        val returnVal = impl()

                        // serialize return value
                        // TODO on error, HttpStatusCode.InternalServerError
                        jsonFormat.stringify(serializer(returnType.type), returnVal)

                    } catch (e: Throwable) {
                        // TODO remove
                        System.err.println("---------------------------------------------------------------------------")
                        e.printStackTrace()
                        System.err.println("---------------------------------------------------------------------------")
                        Thread.sleep(100)
                        throw e
                    }

            // instruct client how to deserialize the response
            call.response.addContentTypeInnerHeader(returnType.type)

            // respond JSON response
            call.respond(TextContent(
                    text = returnJson,
                    contentType = ContentType.Application.Json
            ))
        }

    }

    fun collection(name: String, config: Collection.() -> Unit): Collection =
            Collection(route.createRouteFromPath(name)).apply(config)

}


fun ApplicationResponse.addContentTypeInnerHeader(type: KType) {
    header("X-Content-Type-Inner", type.toString().replace(" ", ""))
}

val Parameters.kvon get() =
        KVON.Pairs(flattenEntries())


@OptIn(ImplicitReflectionSerializer::class, ExperimentalStdlibApi::class)
fun <I : Any> PipelineContext<Unit, ApplicationCall>.readInput(type: ClassedType<I>, jsonFormat: Json): I =
        try {
            when (type) {

                ClassedType.unit ->
                    @Suppress("UNCHECKED_CAST")
                    Unit as I

                else -> {
                    // TODO construct once, when declaring input-taking path
                    val deser = KVONDeserializer(
                            deserializer = type.serializer,
                            jsonFormat = jsonFormat
                    )

                    call.parameters
                            .kvon
                            .let(deser::deserialize)
                }

            }
        } catch (e: Throwable) {
            throw IllegalArgumentException("Error constructing input of type $type from request: $e", e)
        }


