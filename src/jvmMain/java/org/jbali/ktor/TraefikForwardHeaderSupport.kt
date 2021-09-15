package org.jbali.ktor

import io.ktor.features.*
import io.ktor.http.*

/**
 * Config [XForwardedHeaderSupport] to be used specifically behind a Traefik front-end,
 * dealing with its implementation-defined behaviours, which are:
 *
 * - Sending `X-Forwarded-Proto` wss/ws for websockets.
 * - Not sending `X-Forwarded-For` for websockets, only `X-Real-Ip`.
 * - Sending `X-Forwarded-Server` with its own hostname, which is correct, but which [XForwardedHeaderSupport] by
 *   default interprets wrongly as being equivalent to `X-Forwarded-Host`.
 *
 * NOTE: currently assumes that all requests from Traefik were originally submitted
 * over HTTPS/WSS. That also implies Traefik must handle the HTTP -> HTTPS redirect with
 * e.g. `--entrypoints=Name:http Address::80 Redirect.EntryPoint:https`
 */
fun XForwardedHeaderSupport.Config.traefik() {
    hostHeaders -= HttpHeaders.XForwardedServer
    forHeaders += "X-Real-Ip"
    
    // HACK! There's no easy way to do `if scheme == wss then scheme = https`,
    // but we can abuse this feature to always set it for proxied requests.
    // This assumes that all requests from Traefik were originally secure.
//    httpsFlagHeaders += HttpHeaders.XForwardedServer - NOPE, header val must be true
    // TODO proper fix somewhere
    
}