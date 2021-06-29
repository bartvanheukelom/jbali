package org.jbali.jsonrpc

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.serializer
import org.jbali.kotser.DefaultJson
import org.jbali.kotser.std.UUIDSerializer
import org.slf4j.LoggerFactory
import java.util.*

class KtorJsonRPCClient(
    url: Url,
    val client: HttpClient = HttpClient(CIO) {
        if (url.user != null) {
            install(Auth) {
                basic {
                    username = url.user!!
                    password = url.password!!
                }
            }
        }
//        install(Logging) {
//            level = LogLevel.ALL
//        }
    },
    val logging: Boolean = false,
) {
    
    companion object {
        @PublishedApi
        internal val log = LoggerFactory.getLogger(KtorJsonRPCClient::class.qualifiedName!!)
    }
    
    val url = url.copy(
        user = null,
        password = null,
    )
    
    suspend inline fun <reified R, reified E : Any> request(
        method: String,
        noinline build: RequestBuilder.() -> Unit = {},
    ): Either<E, R> =
        request(
            method = method,
            resultSer = serializer(),
            errorSer = serializer(),
            build = build,
        )
    
    suspend fun <R, E : Any> request(
        method: String,
        resultSer: KSerializer<R>,
        errorSer: KSerializer<E>,
        build: RequestBuilder.() -> Unit = {},
    ): Either<E, R> {
        
        val resp: String =
            client.post(url) {
                val req: JsonRPCRequest<UUID, JsonObject> =
                    JsonRPCRequest(
                        method = method,
                        params = RequestBuilder()
                            .apply(build)
                            .requestBody(),
                        id = UUID.randomUUID(),
                    )
                val bodyText = DefaultJson.plain.encodeToString(
                    serializer = JsonRPCRequest.serializer(UUIDSerializer, JsonObject.serializer()),
                    value = req
                )
                if (logging) {
                    log.info("Request: POST ${this@KtorJsonRPCClient.url} $bodyText")
                }
                body =
                    TextContent(
                        text = bodyText,
                        contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    )
            }
        
        if (logging) {
            log.info("Response: $resp")
        }
        
        val jsonResp: JsonRPCResponse<UUID, R, E> =
            try {
                DefaultJson.plain.decodeFromString(
                    deserializer = JsonRPCResponse.serializer(UUIDSerializer, resultSer, errorSer),
                    string = resp,
                )
            } catch (e: Throwable) {
                throw RuntimeException("Error parsing response $resp: $e", e)
            }
        
        return when (val e = jsonResp.error) {
            null -> jsonResp.result.right()
            
            // TODO this is never reached because an expection is thrown by the error response code (at least in bitcoin)
            else -> e.left()
        }
        
    }
    
    inner class RequestBuilder() {
    
        @PublishedApi
        internal val params = mutableMapOf<String, JsonElement>()
        
        inline fun <reified V> param(name: String, value: V) {
            param(
                name = name,
                serializer = serializer(),
                value = value
            )
        }
        
        fun <V> param(
            name: String,
            serializer: SerializationStrategy<V>,
            value: V,
        ) {
            val json = DefaultJson.plain.encodeToJsonElement(serializer, value)
            if (params.putIfAbsent(name, json) != null) {
                throw error("Already have an argument $name")
            }
        }
        
        @PublishedApi
        internal fun requestBody(): JsonObject =
            buildJsonObject {
                params.forEach { (k, v) ->
                    put(k, v)
                }
            }
        
    }
    
}