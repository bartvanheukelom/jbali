package org.jbali.rest

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.http.content.OutgoingContent.ByteArrayContent
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

interface RestRouteContext : RestApiContext {
    
    val route: Route
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("")
    val ktorRouteForHacks get() = route
    
    fun path(name: String, config: RestRoute.() -> Unit): RestRouteContext
    fun exact(config: RestRoute.() -> Unit): Pair<RestRoute.Sub, RestRoute.Sub>
    
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
                call.respondObject(
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
    
    suspend fun <T> ApplicationCall.respondObject(
        returnType: ReifiedType<T>,
        returnVal: T,
        status: HttpStatusCode = HttpStatusCode.OK,
        cacheSerialization: Boolean = true,
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
    private val Any.jsonCacheUntyped: (KSerializer<Any>) -> String
            by StoredExtensionProperty {

                val objStr = toString()
                val cache = "(created @ ${Instant.now()})"

                val obj: WeakReference<Any> = this.weakReference()
                weakKeyLoadingCache<KSerializer<Any>, String> { ser ->
                    val o = obj.get()
                        ?: throw IllegalStateException(
                            "Object collected while using its jsonCache???" +
                            "; objStr=$objStr, cache=$cache; ser=$ser"
                        )
                    jsonFormat.encodeToString(ser, o)
                }
            }
    @Suppress("UNCHECKED_CAST")
    private val <T> T.jsonCache: (KSerializer<T>) -> String get() =
        jsonCacheUntyped as (KSerializer<T>) -> String

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

        call.respondObject(returnType = returnType, returnVal = returnVal)
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
    ) = call.respondObject(returnVal, status)
    
    suspend fun <T> PipelineContext<Unit, ApplicationCall>.respondObject(
        returnType: ReifiedType<T>,
        returnVal: T,
        status: HttpStatusCode
    ) = call.respondObject(returnType, returnVal, status)
    
    suspend inline fun <reified T> ApplicationCall.respondObject(
            returnVal: T,
            status: HttpStatusCode = HttpStatusCode.OK
    ) {
        respondObject(
                status = status,
                returnType = reifiedTypeOf(),
                returnVal = returnVal
        )
    }
    
    
    private val serializers: MutableMap<ReifiedType<*>, CachingSerializer<*>> = ConcurrentHashMap()
    
    private interface ResponseSerializer<T> {
        fun serialize(obj: T): String
    }
    // TODO not inner
    private inner class CachingSerializer<T>(
        private val type: ReifiedType<T>,
        private val ser: KSerializer<T>,
    ) : ResponseSerializer<T> {
        private val serializedResponses: Cache<Any, String> = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(65536)
            .recordStats()
            .build()
        
        init {
            GuavaCacheMetrics.monitor(Metrics.globalRegistry, serializedResponses, "org.jbali.rest.RestRoute.serializedResponses", listOf(
                Tag.of("route", route.toString()),
                Tag.of("type", type.toString()),
            ))
        }
        
        override fun serialize(obj: T): String =
            obj?.let {
                serializedResponses.get(it) {
                    serializeNoCache(it)
                }
            } ?: serializeNoCache(obj)
            
        fun serializeNoCache(obj: T): String =
            Metrics.timer("jbali_rest_response_serialization", listOf(
                Tag.of("route", route.toString()),
                Tag.of("type", type.toString()),
            )).record(Supplier {
                jsonFormat.encodeToString(ser, obj)
            })!!
            
    }
    private inner class MutableResponseSerializer<T>(
        type: ReifiedType<T>,
        val ser: KSerializer<T>,
    ) : ResponseSerializer<T> {
        override fun serialize(obj: T): String =
            jsonFormat.encodeToString(ser, obj)
    }

    override suspend fun <T> ApplicationCall.respondObject(
        returnType: ReifiedType<T>,
        returnVal: T,
        status: HttpStatusCode,
        cacheSerialization: Boolean,
    ) {
        
        if (returnType.extends(reifiedTypeOf<ByteArrayContent>())) {
            respondWithETag(returnVal as ByteArrayContent)
        } else {
            
            val returnJson: String = try {
                
                // get serializer
                @Suppress("UNCHECKED_CAST")
                val ser = serializers.getOrPut(returnType) {
                    
                    val kser = Metrics.timer("jbali_rest_response_serializer_lookup", listOf(
                        Tag.of("route", route.toString()),
                        Tag.of("type", returnType.toString()),
                    )).record(Supplier {
                        jsonFormat.serializersModule.serializer(returnType.type)
                    }) as KSerializer<T>
                    
                    // must always return Caching, even if cacheSerialization is false, because that may change from request to request
                    // TODO fix that
                    CachingSerializer(
                        type = returnType,
                        ser = kser
                    )
                } as CachingSerializer<T>
                
                // serialize return value
                if (cacheSerialization) {
                    ser.serialize(returnVal)
                } else {
                    ser.serializeNoCache(returnVal)
                }
            } catch (e: Exception) {
                log.warn("Error in cached serialization of returnVal: $e")
                jsonFormat.encodeToString(jsonFormat.serializersModule.serializer(returnType.type), returnVal)
            }
            
            // TODO this always throws an exception, fix or remove
    //            try {
    //                returnVal.jsonCache.invoke(returnType.serializer)
    //            } catch (e: Throwable) {
    //                log.warn("Error invoking jsonCache for returnVal=$returnVal, returnType.serializer=${returnType.serializer}", e)
//                    jsonFormat.encodeToString(ser, returnVal)
    //            }
    
            // instruct client how to deserialize the response
            // TODO instead, set contentType to application/vnd.$returnType+json... but how to deal with nullability and type parameters
            response.addContentTypeInnerHeader(returnType.type)
    
            // respond JSON response
            respondJson(
                    json = returnJson,
                    status = status
            )
        }
    }

    private suspend fun ApplicationCall.respondJson(
            json: String,
            status: HttpStatusCode = HttpStatusCode.OK,
//            contentType: ContentType = ContentType.Application.Json
    ) {

        // disabled because that's the caller's own responsibility
//        contentType.requireJson()

        respondWithETag(TextContent(
                status = status,
                text = json,
                contentType = ContentType.Application.Json
        ))
    }

    // TODO can this be done as an intercept?
    suspend fun ApplicationCall.respondWithETag(c: ByteArrayContent) {

        when (request.httpMethod) {
            HttpMethod.Get, HttpMethod.Head ->
                // TODO quote (or use conditionalheaders feature correctly)
                response.etag(c.etag)
        }

        respond(c)
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
    
    override fun exact(config: RestRoute.() -> Unit): Pair<Sub, Sub> =
        route.routeExact {
            Sub(
                context = context,
                route = this,
            )
                .configure(config)
        }
        

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
