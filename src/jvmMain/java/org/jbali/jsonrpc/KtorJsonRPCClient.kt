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
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.serializer
import org.jbali.kotser.DefaultJson
import org.jbali.kotser.std.UUIDSerializer
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
) {
    
    val url = url.copy(
        user = null,
        password = null,
    )
    
    suspend inline fun <reified R, reified E : Any> request(
        method: String,
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
                body =
                    TextContent(
                        text = DefaultJson.plain.encodeToString(
                            serializer = JsonRPCRequest.serializer(UUIDSerializer, JsonObject.serializer()),
                            value = req
                        ),
                        contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    )
            }
        
        val jsonResp: JsonRPCResponse<UUID, R, E> =
            try {
                DefaultJson.plain.decodeFromString(
                    deserializer = JsonRPCResponse.serializer(UUIDSerializer, serializer(), serializer()),
                    string = resp,
                )
            } catch (e: Throwable) {
                throw RuntimeException("Error parsing response $resp: $e", e)
            }
        
        return when (val e = jsonResp.error) {
            null -> jsonResp.result.right()
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