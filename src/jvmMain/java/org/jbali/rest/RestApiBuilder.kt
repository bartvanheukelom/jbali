package org.jbali.rest

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
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
import org.jbali.kotser.put
import org.jbali.ktor.BasicErrorException
import org.jbali.ktor.handleAnyPath
import org.jbali.ktor.routeExact
import org.jbali.ktor.uuidOrNull
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
        val tmpFiller: Int = 1,
        val oas: OpenAPI,
)

data class RestApiBuilder(
    override val route: Route,
    override val jsonFormat: Json,
    private val callIdGetter: ApplicationCall.() -> Any? = { uuidOrNull }
) : RestRoute(), RestApiContext {

    // TODO api name or route
    private val log: Logger = LoggerFactory.getLogger(RestApiBuilder::class.java)

    override val context get() = this
    override val restRoute get() = this

    private val oasPaths = mutableMapOf<String, PathItem>()

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
                    log.warn("Rest Api call #${call.callIdGetter()} Exception ${e.uuid}", e)

                    when (e) {

                        is RestException ->
                            // TODO serialize to nice JSON (only for JSON requests?)
                            call.respondWithETag(TextContent(
                                    status = e.statusCode,
                                    contentType = ContentType.Text.Plain,
                                    text = e.stackTraceString // TODO hide
                            ))

                        is BasicErrorException ->
                            call.respond(e)

                        else ->
                            // TODO serialize to nice JSON (only for JSON requests?)
                            call.respondWithETag(TextContent(
                                    status = HttpStatusCode.InternalServerError,
                                    contentType = ContentType.Text.Plain,
                                    text = e.stackTraceString // TODO hide
                            ))

                    }

                }
            }

            oasPath("", PathItem(
                get = Operation(
                    responses = mapOf(
                        HttpStatusCode.OK.value.toString() to Response(
                            description = "root hi",
                            content = buildJsonObject {
                                put(ContentType.Application.Json.contentType, buildJsonObject {
                                    put("schema", buildJsonObject {
                                        put("type", "object")
                                        put("properties", buildJsonObject {
                                            put("hello", buildJsonObject {
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

            routeExact {
                get {
                    respondObject(buildJsonObject {
                        put("hello", "this is api")
                    })
                }
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
        route.handleAnyPath {
            respondObject(
                status = HttpStatusCode.NotFound,
                returnVal = buildJsonObject {
                    put("message", jsonString("'${call.request.path()}' not found in REST API @ $route"))
                    errorResponseAugmenter(call)
                }
            )
        }

        return RestApi(
            oas = oas
        )
    }
    
    override fun oasPath(path: String, item: PathItem) {
        oasPaths.compute("/$path") { _, existing ->
            existing?.plus(item) ?: item
        }
    }

}

fun Route.restApi(
        jsonFormat: Json = DefaultJson.indented,
        callIdGetter: ApplicationCall.() -> Any? = { uuidOrNull },

        config: RestApiBuilder.() -> Unit
): RestApi =
        RestApiBuilder(
                route = this,
            
                jsonFormat = jsonFormat,
                callIdGetter = callIdGetter,
        )
                .apply(config)
                .build()


operator fun PathItem.plus(b: PathItem): PathItem =
    PathItem(
        delete = delete ?: b.delete,
        description = description ?: b.description,
        get = get ?: b.get,
        head = head ?: b.head,
        options = options ?: b.options,
        parameters = parameters ?: b.parameters,
        patch = patch ?: b.patch,
        post = post ?: b.post,
        put = put ?: b.put,
        servers = servers ?: b.servers,
        summary = summary ?: b.summary,
        trace = trace ?: b.trace,
    )