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
import org.jbali.ktor.wsToHttp
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
    @OptIn(InternalAPI::class, KtorExperimentalAPI::class)
    object : HttpClientEngineFactory<T> {
        override fun create(block: T.() -> Unit): HttpClientEngine {
            val org: HttpClientEngine = this@withRequestModder.create(block)
            return object : HttpClientEngine {

                override suspend fun execute(data: HttpRequestData): HttpResponseData =
                    org.execute(data.modder())
   
                // Note how this class doesn't do ` : HttpClientEngine by org`.
                // That's because it must not do this:
//                override fun install(client: HttpClient) { org.install(client) }

                // So do manual delegation:
                override val config: HttpClientEngineConfig
                    get() = org.config
                override val coroutineContext: CoroutineContext
                    get() = org.coroutineContext
                override val dispatcher: CoroutineDispatcher
                    get() = org.dispatcher
                override fun close() {
                    org.close()
                }
                override val supportedCapabilities get() = org.supportedCapabilities

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
 *
 * @param forwardWs For `ws(s)://..` URLs, whether `X-Forwarded-Proto` is set to `ws(s)` (true) or `http(s)` (false).
 *                  It's not exactly clear what is proper. The default is true because that seems to cause the most
 *                  backend issues, and is therefore most useful for testing.
 */
fun HttpRequestData.proxyPass(
    backendUrl: Url,
    clientAddress: InetAddress,
    forwardWs: Boolean = true,
    forwardFor: Boolean = true,
    moreHeaders: Map<String, String> = emptyMap(),
) =
    copy(
        url = backendUrl,
        headers = headers
                + mapOf(
                    HttpHeaders.XForwardedProto to url.protocol
                        .let { if (forwardWs) it else it.wsToHttp() }
                        .name,
                    "X-Real-Ip" to clientAddress.hostAddress,
                    HttpHeaders.XForwardedHost to url.hostWithPort,
                    HttpHeaders.XForwardedServer to "frontend.biz",
                )
                + (if (forwardFor) {
                      mapOf(HttpHeaders.XForwardedFor to clientAddress.hostAddress)
                  } else emptyMap())
                + moreHeaders,
    )

fun HttpRequestData.proxyLikeTraefik1(
    backendUrl: Url,
    clientAddress: InetAddress,
) =
    proxyPass(
        backendUrl = backendUrl,
        clientAddress = clientAddress,
        forwardWs = true,
        forwardFor = !url.protocol.isWebsocket(),
        moreHeaders = mapOf(
            "X-Forwarded-SSL" to "true"
        )
    )
