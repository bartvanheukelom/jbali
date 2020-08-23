package org.jbali.rest

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent.ByteArrayContent
import io.ktor.http.content.TextContent
import io.ktor.request.httpMethod
import io.ktor.response.etag
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.createRouteFromPath
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.KSerializer
import org.jbali.bytes.Hex
import org.jbali.bytes.encodedAs
import org.jbali.crypto.sha256
import org.jbali.kotser.serializer
import org.jbali.util.ReifiedType
import org.jbali.util.StoredExtensionProperty
import org.jbali.util.reifiedTypeOf
import org.jbali.util.weakKeyLoadingCache
import kotlin.contracts.ExperimentalContracts

abstract class RestRoute : RestApiContext {

    abstract val route: Route

    // TODO cache TextContent, or at least the bytes
    val <T> T.asJsonCache: (KSerializer<T>) -> String
            by StoredExtensionProperty {
                val obj = this
                weakKeyLoadingCache<KSerializer<T>, String> { ser ->
                    jsonFormat.stringify(ser, obj)
                }
            }

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

    suspend fun <T> PipelineContext<Unit, ApplicationCall>.rawHandle(
            returnType: ReifiedType<T>,
            impl: suspend () -> T
    ) {

        // call implementation
        val returnVal = impl()

        respondObject(returnType = returnType, returnVal = returnVal)
    }

    suspend inline fun <reified T> PipelineContext<Unit, ApplicationCall>.respondObject(
            returnVal: T,
            status: HttpStatusCode = HttpStatusCode.OK
    ) {
        respondObject(
                status = status,
                returnType = reifiedTypeOf(),
                returnVal = returnVal
        )
    }

    suspend fun <T> PipelineContext<Unit, ApplicationCall>.respondObject(
            returnType: ReifiedType<T>,
            returnVal: T,
            status: HttpStatusCode = HttpStatusCode.OK
    ) {

        // serialize return value
        val returnJson: String = returnVal.asJsonCache.invoke(returnType.serializer)

        // instruct client how to deserialize the response
        call.response.addContentTypeInnerHeader(returnType.type)

        // respond JSON response
        respondJson(
                json = returnJson,
                status = status
        )
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.respondJson(
            json: String,
            status: HttpStatusCode = HttpStatusCode.OK
    ) {
        respond(TextContent(
                status = status,
                text = json,
                contentType = ContentType.Application.Json
        ))
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.respond(c: ByteArrayContent) {

        when (call.request.httpMethod) {
            HttpMethod.Get, HttpMethod.Head ->
                // TODO quote (or use conditionalheaders feature correctly)
                call.response.etag(c.etag)
        }

        call.respond(c)
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

@OptIn(ExperimentalContracts::class)
inline operator fun <T, R> T.div(block: (T) -> R): R {
    return block(this)
}

private val ByteArrayContent.etag: String by StoredExtensionProperty {
    bytes() /
            { it.sha256 } /
            { it encodedAs Hex.Lower } /
            { it.string }
}
