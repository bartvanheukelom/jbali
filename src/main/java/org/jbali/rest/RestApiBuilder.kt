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
import io.ktor.routing.route
import io.ktor.serialization.serialization
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.json
import org.jbali.errors.removeCommonStack
import org.jbali.errors.stackTraceString
import org.jbali.kotser.DefaultJson
import org.jbali.ktor.BasicErrorException
import org.jbali.oas.*
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

class RestException(val statusCode: HttpStatusCode, message: String? = null, cause: Throwable? = null) :
        RuntimeException(
                buildString {
                    append("Rest exception $statusCode")
                    message?.let { append(": $it") }
                    cause?.let { append(": $it") }
                },
                cause
        ) {

    constructor(statusCode: HttpStatusCode, cause: Throwable) : this(statusCode, null, cause)
    constructor(statusCode: HttpStatusCode, message: String) : this(statusCode, message, null)

}

data class RestApi(
        val oas: OpenAPI
)

@OptIn(ExperimentalStdlibApi::class)
data class RestApiBuilder(
        override val route: Route,
        override val jsonFormat: Json
) : RestRoute(), RestApiContext {

    // TODO api name or route
    private val log: Logger = LoggerFactory.getLogger(RestApiBuilder::class.java)

    override val context get() = this

    private val oasPaths = mutableMapOf<String, PathItem>()

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

                    when (e) {

                        is RestException ->
                            // TODO serialize to nice JSON (only for JSON requests?)
                            call.respond(TextContent(
                                    status = e.statusCode,
                                    contentType = ContentType.Text.Plain,
                                    text = e.stackTraceString // TODO hide
                            ))

                        is BasicErrorException ->
                            call.respond(e)

                        else ->
                            // TODO serialize to nice JSON (only for JSON requests?)
                            call.respond(TextContent(
                                    status = HttpStatusCode.InternalServerError,
                                    contentType = ContentType.Text.Plain,
                                    text = e.stackTraceString // TODO hide
                            ))

                    }

                }
            }

            oasPaths["/"] = PathItem(
                get = Operation(
                        responses = mapOf(
                                HttpStatusCode.OK.value.toString() to Response(
                                        description = "root hi",
                                        content = json {
                                            ContentType.Application.Json.contentType to json {
                                                "schema" to json {
                                                    "type" to "object"
                                                    "properties" to json {
                                                        "hello" to json {
                                                            "type" to "string"
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                )
                        )
                )
            )

            get("") {
                respondObject(json {
                    "hello" to "this is api"
                })
            }
        }
    }

    fun build(): RestApi {
        val oas = OpenAPI(
                openapi = "3.0.3",
                info = Info(
                        title = "rest api",
                        version = "0.1"
                ),
                paths = oasPaths
        )

        route.get("oas") {
            respondObject(oas)
        }

        // default to 404 if no matches
        route.route("{...}") {
            handle {
                respondObject(
                        status = HttpStatusCode.NotFound,
                        returnVal = "Not Found"
                )
            }
        }

        return RestApi(
                oas = oas
        )
    }

}

fun Route.restApi(
        jsonFormat: Json = DefaultJson.indented,

        config: RestApiBuilder.() -> Unit
): RestApi =
        RestApiBuilder(
                route = this,
                jsonFormat = jsonFormat
        )
                .apply(config)
                .build()
