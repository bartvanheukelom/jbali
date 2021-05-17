package org.jbali.ktor.client

import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import org.jbali.ktor.plus
import java.net.InetAddress
import kotlin.coroutines.CoroutineContext

/**
 * Extend this [HttpClientEngineFactory] to produce an [HttpClientEngine] that
 * allows [modder] to modify the [HttpRequestData] just before it's executed,
 * without those modifications being visible in e.g. [HttpResponse.request].
 */
fun <T : HttpClientEngineConfig> HttpClientEngineFactory<T>.withRequestModder(
    modder: HttpRequestData.() -> HttpRequestData
) =
    @OptIn(InternalAPI::class)
    object : HttpClientEngineFactory<T> {
        override fun create(block: T.() -> Unit): HttpClientEngine {
            val org: HttpClientEngine = this@withRequestModder.create(block)
            return object : HttpClientEngine
//                by org - TODO doesn't work, override execute is not called. why?
            {

                override suspend fun execute(data: HttpRequestData): HttpResponseData =
                    org.execute(data.modder())

                // manual delegation:
                override val config: HttpClientEngineConfig
                    get() = org.config
                override val coroutineContext: CoroutineContext
                    get() = org.coroutineContext
                override val dispatcher: CoroutineDispatcher
                    get() = org.dispatcher
                override fun close() {
                    org.close()
                }

            }
        }

    }


fun HttpRequestData.copy(
    url: Url = this.url,
    method: HttpMethod = this.method,
    headers: Headers = this.headers,
    body: OutgoingContent = this.body,
    executionContext: Job = this.executionContext,
    attributes: Attributes = this.attributes,
): HttpRequestData =
    @OptIn(InternalAPI::class)
    HttpRequestData(
        url = url,
        method = method,
        headers = headers,
        body = body,
        executionContext = executionContext,
        attributes = attributes,
    )


/**
 * Transform this request as if a reverse proxy is forwarding it to the given
 * [backendUrl], with the request originating from [clientAddress].
 *
 * Specifically, changes the [HttpRequestData.url] to [backendUrl],
 * and adds the original url and given [clientAddress] as `X-Forwarded` headers.
 */
fun HttpRequestData.proxyPass(
    backendUrl: Url,
    clientAddress: InetAddress,
) =
    copy(
        url = backendUrl,
        headers = headers + mapOf(
            HttpHeaders.XForwardedProto to url.protocol.name,
            HttpHeaders.XForwardedFor to clientAddress.hostAddress,
            HttpHeaders.XForwardedHost to url.hostWithPort,
        ),
    )
