package org.jbali.rest

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.http.content.OutgoingContent.ByteArrayContent
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.serializer
import org.jbali.bytes.Hex
import org.jbali.bytes.encodedAs
import org.jbali.crypto.sha256
import org.jbali.kotser.jsonString
import org.jbali.ktor.handleExact
import org.jbali.ktor.respondNoContent
import org.jbali.ktor.routeExact
import org.jbali.util.ReifiedType
import org.jbali.util.StoredExtensionProperty
import org.jbali.util.reifiedTypeOf
import org.jbali.util.weakKeyLoadingCache
import org.slf4j.LoggerFactory
import java.lang.ref.WeakReference
import java.time.Instant

interface RestRouteContext : RestApiContext {
    val ktorRouteForHacks: Route
    fun path(name: String, config: RestRoute.() -> Unit): RestRouteContext
    
    fun <T> post(
        path: String,
        impl: suspend (ApplicationCall) -> T
    ) {
        ktorRouteForHacks.createRouteFromPath(path).routeExact {
    
            post {
                impl(call)
            }
            
            handle {
                call.response.header(HttpHeaders.Allow, HttpMethod.Post.value)
                respondObject(
                    returnType = reifiedTypeOf(),
                    status = HttpStatusCode.MethodNotAllowed,
                    returnVal = buildJsonObject {
                        put("message", jsonString("Method Not Allowed: ${call.request.httpMethod.value}"))
                        errorResponseAugmenter(call)
                    }
                )
            }
        }
    }
    
    suspend fun <T> PipelineContext<Unit, ApplicationCall>.respondObject(
        returnType: ReifiedType<T>,
        returnVal: T,
        status: HttpStatusCode = HttpStatusCode.OK
    )
}

inline fun <reified I : Any> RestRouteContext.post(
    path: String,
    noinline impl: suspend (ApplicationCall, I) -> Unit
) {
    post(
        path = path,
        inputType = reifiedTypeOf(),
        impl = impl
    )
}
fun <I : Any> RestRouteContext.post(
    path: String,
    inputType: ReifiedType<I>,
    impl: suspend (ApplicationCall, I) -> Unit
) {
    post(path) { call ->
        impl(call, call.receive(inputType.type))
        // TODO base on return type of impl:
        //      - Unit -> No Content
        //      - special redirect object
        //      - some value
        call.respondNoContent()
    }
}


abstract class RestRoute : RestRouteContext {

    private val log = LoggerFactory.getLogger(RestRoute::class.java)

    internal abstract val route: Route

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("")
    override val ktorRouteForHacks get() = route

    protected val allowedMethods = mutableSetOf<HttpMethod>()

    /**
     * For the given object, returns a function C that, when called with a [KSerializer],
     * returns the JSON representation of the object, using that serializer, and the [jsonFormat] of this route.
     *
     * _Memory_
     *
     * - 1 instance of [StoredExtensionProperty] per [RestRoute].
     * - 1 instance of C for each instance of [T] in each [StoredExtensionProperty].
     * - Each C refers to another cache, which associates (WeakReferenced) [KSerializer]s with JSON.
     */
    // TODO cache TextContent, or at least the bytes.
    // TODO the assumption made the in StoredExtensionProperty implementation, that the delegates themselves are basically
    //      static, is now proven false. theoretically, could leak memory if rest routes are created and removed repeatedly.
    private val <T> T.jsonCache: (KSerializer<T>) -> String
            by StoredExtensionProperty {

                val objStr = toString()
                val cache = "(created @ ${Instant.now()})"

                val obj: WeakReference<T> = this.weakReference()
                weakKeyLoadingCache<KSerializer<T>, String> { ser ->
                    val o = obj.get()
                        ?: throw IllegalStateException(
                            "Object collected while using its jsonCache???" +
                            "; objStr=$objStr, cache=$cache; ser=$ser"
                        )
                    jsonFormat.encodeToString(ser, o)
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

    /**
     * Let [impl] handle this call without any input processing, but do process its return value
     * as a response object.
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("obfuscates too much, provides little value")
    suspend fun <T> PipelineContext<Unit, ApplicationCall>.handleCustomInput(
            returnType: ReifiedType<T>,
            impl: suspend () -> T
    ) {

        // call implementation
        val returnVal = impl()

        respondObject(returnType = returnType, returnVal = returnVal)
    }

    /**
     * Let [impl] handle this call without any input processing, but do process its return value
     * as a response object.
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("obfuscates too much, provides little value")
    suspend inline fun <reified T : Any> PipelineContext<Unit, ApplicationCall>.handleCustomInput(
        noinline impl: suspend () -> T
    ) {
        @Suppress("DEPRECATION")
        handleCustomInput(
            returnType = reifiedTypeOf(),
            impl = impl
        )
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

    override suspend fun <T> PipelineContext<Unit, ApplicationCall>.respondObject(
            returnType: ReifiedType<T>,
            returnVal: T,
            status: HttpStatusCode
    ) {
        
        if (returnType.extends(reifiedTypeOf<ByteArrayContent>())) {
            respond(returnVal as ByteArrayContent)
        } else {
    
            // serialize return value
            val returnJson: String =
                // TODO fix
    //            try {
    //                returnVal.jsonCache.invoke(returnType.serializer)
    //            } catch (e: Throwable) {
    //                log.warn("Error invoking jsonCache for returnVal=$returnVal, returnType.serializer=${returnType.serializer}", e)
                    jsonFormat.encodeToString(jsonFormat.serializersModule.serializer(returnType.type), returnVal)
    //            }
    
            // instruct client how to deserialize the response
            // TODO instead, set contentType to application/vnd.$returnType+json... but how to deal with nullability and type parameters
            call.response.addContentTypeInnerHeader(returnType.type)
    
            // respond JSON response
            respondJson(
                    json = returnJson,
                    status = status
            )
        }
    }

    private suspend fun PipelineContext<Unit, ApplicationCall>.respondJson(
            json: String,
            status: HttpStatusCode = HttpStatusCode.OK,
//            contentType: ContentType = ContentType.Application.Json
    ) {

        // disabled because that's the caller's own responsibility
//        contentType.requireJson()

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

    open class Sub(
            context: RestApiContext,
            override val route: Route
    ) : RestRoute(), RestApiContext by context

    override fun path(name: String, config: RestRoute.() -> Unit): Sub =
            Sub(
                    context = context,
                    route = route.createRouteFromPath(name)
            )
                .configure(config)

    fun postConfig() {
        setupMethodNotAllowed()
    }

    private fun setupMethodNotAllowed() {

        val allowedHeader =
            allowedMethods.joinToString { it.value }

        route.handleExact {

            call.response.header(HttpHeaders.Allow, allowedHeader)

            respondObject(
                status = HttpStatusCode.MethodNotAllowed,
                returnVal = buildJsonObject {
                    put("message", jsonString("Method Not Allowed: ${call.request.httpMethod.value}"))
                    errorResponseAugmenter(call)
                }
            )
        }
    }

}

inline fun <R : RestRoute> R.configure(config: R.() -> Unit): R =
    this
        .apply(config)
        .apply(RestRoute::postConfig)

//@OptIn(ExperimentalContracts::class)
inline operator fun <L, R> L.div(block: (L) -> R): R {
    // TODO enable when allowed for operator functions:
//    contract {
//        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
//    }
    return block(this)
}

private val ByteArrayContent.etag: String
    by StoredExtensionProperty {
        this()
            .bytes()
            .sha256
            .encodedAs(Hex.Lower)
            .string
    }
