package org.jbali.rest

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.createRouteFromPath
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.serializer
import org.jbali.util.ReifiedType
import org.jbali.util.reifiedTypeOf

abstract class RestRoute : RestApiContext {

    abstract val route: Route

    inline fun <reified I : Any> PipelineContext<Unit, ApplicationCall>.readInput(): I =
            readInput(type = reifiedTypeOf())

    fun <I : Any> PipelineContext<Unit, ApplicationCall>.readInput(type: ReifiedType<I>): I =
            try {
                // compose input
                readInput(
                        type = type,
                        jsonFormat = jsonFormat
                )
            } catch (e: Throwable) {
                throw RestException(HttpStatusCode.BadRequest, e)
            }

    suspend fun <T : Any> PipelineContext<Unit, ApplicationCall>.rawHandle(
            returnType: ReifiedType<T>,
            impl: suspend () -> T
    ) {

        // call implementation
        val returnVal = impl()

        respondObject(returnType, returnVal)
    }

    suspend inline fun <reified T : Any> PipelineContext<Unit, ApplicationCall>.respondObject(returnVal: T) {
        respondObject(
                returnType = reifiedTypeOf(),
                returnVal = returnVal
        )
    }

    suspend fun <T : Any> PipelineContext<Unit, ApplicationCall>.respondObject(returnType: ReifiedType<T>, returnVal: T) {
        // serialize return value
        val returnJson = jsonFormat.stringify(serializer(returnType.type), returnVal)

        // instruct client how to deserialize the response
        call.response.addContentTypeInnerHeader(returnType.type)

        // respond JSON response
        respondJson(returnJson)
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.respondJson(json: String) {
        call.respond(TextContent(
                text = json,
                contentType = ContentType.Application.Json
        ))
    }

    suspend inline fun <reified T : Any> PipelineContext<Unit, ApplicationCall>.rawHandle(
            noinline impl: suspend () -> T
    ) {
        rawHandle(
                returnType = reifiedTypeOf(),
                impl = impl
        )
    }

    open class Sub(
            context: RestApiContext,
            override val route: Route
    ) : RestRoute(), RestApiContext by context

    fun path(name: String, config: RestRoute.() -> Unit): Sub =
            Sub(
                    context = context,
                    route = route.createRouteFromPath(name)
            )
                    .apply(config)

}