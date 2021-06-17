package org.jbali.jsonrpc

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
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
        install(Logging) {
            level = LogLevel.ALL
        }
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
        
        // TODO how to handle HTTP error codes.
        val jsonResp: JsonRPCResponse<UUID, R, E> =
            DefaultJson.plain.decodeFromString(
                deserializer = JsonRPCResponse.serializer(UUIDSerializer, serializer(), serializer()),
                string = resp,
            )
        
        return when (val e = jsonResp.error) {
            null -> jsonResp.result.right()
            else -> e.left()
        }
        
    }
    
    inner class RequestBuilder() {
    
        private val params = mutableMapOf<String, JsonElement>()
        
        @PublishedApi
        internal fun requestBody(): JsonObject =
            buildJsonObject {
                params.forEach { (k, v) ->
                    put(k, v)
                }
            }
        
    }
    
}