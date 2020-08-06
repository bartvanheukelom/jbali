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
import org.jbali.util.ClassedType
import org.jbali.util.classedTypeOf

abstract class RestRoute : RestApiContext {

    abstract val route: Route

    @ExperimentalStdlibApi
    inline fun <reified I : Any> PipelineContext<Unit, ApplicationCall>.readInput(): I =
            readInput(type = classedTypeOf())

    fun <I : Any> PipelineContext<Unit, ApplicationCall>.readInput(type: ClassedType<I>): I =
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
            returnType: ClassedType<T>,
            impl: suspend () -> T
    ) {

        // call implementation
        val returnVal = impl()

        // serialize return value
        val returnJson = jsonFormat.stringify(serializer(returnType.type), returnVal)

        // instruct client how to deserialize the response
        call.response.addContentTypeInnerHeader(returnType.type)

        // respond JSON response
        call.respond(TextContent(
                text = returnJson,
                contentType = ContentType.Application.Json
        ))
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