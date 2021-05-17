package org.jbali.rest

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import org.jbali.errors.removeCommonStack
import org.jbali.errors.stackTraceString
import org.jbali.kotser.DefaultJson
import org.jbali.kotser.jsonString
import org.jbali.ktor.BasicErrorException
import org.jbali.ktor.handleAnyPath
import org.jbali.ktor.routeExact
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
    val errorResponseAugmenter: JsonObjectBuilder.(ApplicationCall) -> Unit
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
        val tmpFiller: Int = 1
//        val oas: OpenAPI
)

@OptIn(ExperimentalStdlibApi::class)
data class RestApiBuilder(
        override val route: Route,
        override val jsonFormat: Json
) : RestRoute(), RestApiContext {

    // TODO api name or route
    private val log: Logger = LoggerFactory.getLogger(RestApiBuilder::class.java)

    override val context get() = this

//    private val oasPaths = mutableMapOf<String, PathItem>()

    override var errorResponseAugmenter: JsonObjectBuilder.(ApplicationCall) -> Unit = {}

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

//            oasPaths["/"] = PathItem(
//                get = Operation(
//                        responses = mapOf(
//                                HttpStatusCode.OK.value.toString() to Response(
//                                        description = "root hi",
//                                        content = buildJsonObject {
//                                            ContentType.Application.Json.contentType to buildJsonObject {
//                                                "schema" to buildJsonObject {
//                                                    "type" to "object"
//                                                    "properties" to buildJsonObject {
//                                                        "hello" to buildJsonObject {
//                                                            "type" to "string"
//                                                        }
//                                                    }
//                                                }
//                                            }
//                                        }
//                                )
//                        )
//                )
//            )

            routeExact {
                get {
                    respondObject(buildJsonObject {
                        "hello" to "this is api"
                    })
                }
            }
        }
    }

    fun build(): RestApi {
//        val oas = OpenAPI(
//                openapi = "3.0.3",
//                info = Info(
//                        title = "rest api",
//                        version = "0.1"
//                ),
//                paths = oasPaths
//        )
//
//        route.get("oas") {
//            respondObject(oas)
//        }

        // default to 404 if no matches
        route.handleAnyPath {
            respondObject(
                status = HttpStatusCode.NotFound,
                returnVal = buildJsonObject {
                    put("message", jsonString("Not Found in REST API @ $route"))
                    errorResponseAugmenter(call)
                }
            )
        }

        return RestApi(
//                oas = oas
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
