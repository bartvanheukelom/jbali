package org.jbali.rest

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.serialization.serialization
import kotlinx.serialization.json.Json
import org.jbali.errors.removeCommonStack
import org.jbali.errors.stackTraceString
import org.jbali.kotser.DefaultJson
import org.jbali.util.uuid
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Each component of a [RestApiBuilder] implements this interface
 * so that its useful properties, such as [jsonFormat], are available without qualification.
 *
 * Most components implement this interface by delegation to [context],
 * which in practice refers to [RestApiBuilder].
 */
interface RestApiContext {
    val context: RestApiContext
    // useful properties:
    val jsonFormat: Json
}

class RestException(val statusCode: HttpStatusCode, cause: Throwable) :
        RuntimeException("Rest exception $statusCode: $cause", cause)

data class RestApiBuilder(
        override val route: Route,
        override val jsonFormat: Json
) : RestRoute(), RestApiContext {

    // TODO api name or route
    private val log: Logger = LoggerFactory.getLogger(RestApiBuilder::class.java)

    override val context get() = this

    init {
        route.apply {
            // TODO now that this exists, can return objects directly?
            install(ContentNegotiation) {
                serialization(
                        contentType = ContentType.Application.Json,
                        format = jsonFormat
                )
            }
            install(StatusPages) {
                exception { e: Throwable ->
                    e.removeCommonStack()
                    log.warn("Rest Api Exception ${e.uuid}", e)

                    // TODO serialize to nice JSON
                    call.respond(TextContent(
                            status = when (e) {
                                is RestException -> e.statusCode
                                else -> HttpStatusCode.InternalServerError
                            },
                            contentType = ContentType.Text.Plain,
                            text = e.stackTraceString
                    ))
                }
            }

            get("") {
                call.respond("Hello from the REST API")
            }
        }
    }


}

fun Route.restApi(
        jsonFormat: Json = DefaultJson.indented,

        config: RestApiBuilder.() -> Unit
) =
        RestApiBuilder(
                route = this,
                jsonFormat = jsonFormat
        ).also { it.config() }
