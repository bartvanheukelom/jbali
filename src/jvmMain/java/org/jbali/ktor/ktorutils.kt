package org.jbali.ktor

import com.google.common.net.InetAddresses
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.util.*
import java.net.InetAddress
import java.net.UnknownHostException

operator fun <T : Any> Attributes.set(key: AttributeKey<T>, value: T) {
    put(key, value)
}

operator fun Headers.plus(rhs: Map<String, String>) =
    HeadersBuilder()
        .apply {
            appendAll(this@plus)
            rhs.forEach(::append)
        }
        .build()


private val reverseProxyHeaders = listOf(
    HttpHeaders.Forwarded,
    HttpHeaders.XForwardedFor,
    HttpHeaders.XForwardedHost,
    HttpHeaders.XForwardedProto,
    HttpHeaders.XForwardedServer,
    "X-Real-IP",
)

val RequestConnectionPoint.remoteIp: InetAddress
    get() = InetAddresses.forString(remoteHost)

val RequestConnectionPoint.remoteIpOrNull: InetAddress?
    get() = try {
        remoteIp
    } catch (e: IllegalArgumentException) {
        null
    }

val InetAddress.isLocalAddress: Boolean
    get() = isSiteLocalAddress || isLoopbackAddress || isLinkLocalAddress


/**
 * Returns true if the request came from a private IPv4 address, like 192.168.x, 10.x, 172.16.x, etc,
 * and doesn't have any headers that indicate it came from a load balancer / reverse proxy.
 */
val ApplicationCall.isFromInternalNetwork get() =
    request.local.resolvedRemoteIp().second?.isLocalAddress == true && request.headers.names().none { it in reverseProxyHeaders }

// TODO version that checks at origin. optionally verify that local is also internal, if reverse proxy headers are not trusted.


fun RequestConnectionPoint.resolvedRemoteIp(): Pair<String?, InetAddress?> {
    val ip = remoteIpOrNull
    if (ip != null) {
        return Pair(null, ip)
    } else {
        try {
            return Pair(remoteHost, InetAddress.getByName(remoteHost))
        } catch (e: UnknownHostException) {
            return Pair(remoteHost, null)
        } catch (e: Exception) {
//            log.warn("Error resolving remote host '$remoteHost'", e) - TODO
            return Pair(remoteHost, null)
        }
    }
}
