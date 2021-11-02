package org.jbali.ktor

/*
* Adapted from the Ktor 1.6.5 io.ktor.features.CORS code which is:
*
*     Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.*
import io.ktor.util.pipeline.*

/**
 * Extended version of [io.ktor.features.CORS] that's less rigid and easier to adapt to your needs.
 *
 * Added features:
 * - anyHost() and allowCredentials may be used together. If so, the interceptor will allow the origin of the specific request instead of always responding with "*".
 */
class BetterCORS(configuration: Configuration) {
    private val numberRegex = "[0-9]+".toRegex()
    
    /**
     * Allow requests from the same origin
     */
    val allowSameOrigin: Boolean = configuration.allowSameOrigin
    
    /**
     * Allow requests from any origin
     */
    val allowsAnyHost: Boolean = "*" in configuration.hosts
    
    /**
     * Allow to pass credentials
     */
    val allowCredentials: Boolean = configuration.allowCredentials
    
    /**
     * All allowed headers to be sent including simple
     */
    val allHeaders: Set<String> =
        (configuration.headers + Configuration.CorsSimpleRequestHeaders).let { headers ->
            if (configuration.allowNonSimpleContentTypes) headers else headers.minus(HttpHeaders.ContentType)
        }
    
    /**
     * Prefix for permitted headers
     */
    val headerPredicates: List<(String) -> Boolean> = configuration.headerPredicates
    
    /**
     * All allowed HTTP methods
     */
    val methods: Set<HttpMethod> = HashSet<HttpMethod>(configuration.methods + Configuration.CorsDefaultMethods)
    
    /**
     * Set of all allowed headers
     */
    val allHeadersSet: Set<String> = allHeaders.map { it.lowercase() }.toSet()
    
    private val allowNonSimpleContentTypes: Boolean = configuration.allowNonSimpleContentTypes
    
    private val headersList =
        configuration.headers.filterNot { it in Configuration.CorsSimpleRequestHeaders }
            .let { if (allowNonSimpleContentTypes) it + HttpHeaders.ContentType else it }
    
    private val methodsListHeaderValue =
        methods.filterNot { it in Configuration.CorsDefaultMethods }
            .map { it.value }
            .sorted()
            .joinToString(", ")
    
    private val maxAgeHeaderValue = configuration.maxAgeInSeconds.let { if (it > 0) it.toString() else null }
    private val exposedHeaders = when {
        configuration.exposedHeaders.isNotEmpty() -> configuration.exposedHeaders.sorted().joinToString(", ")
        else -> null
    }
    
    private val hostsNormalized =
        HashSet<String>(
            configuration.hosts
                .filterNot { it.contains('*') }
                .map { normalizeOrigin(it) }
        )
    
    private val hostsWithWildcard =
        HashSet<Pair<String, String>>(
            configuration.hosts
                .filter { it.contains('*') }
                .map {
                    val normalizedOrigin = normalizeOrigin(it)
                    val (prefix, suffix) = normalizedOrigin.split('*')
                    prefix to suffix
                }
        )
    
    /**
     * Feature's call interceptor that does all the job. Usually there is no need to install it as it is done during
     * feature installation
     */
    suspend fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        val call = context.call
        
        if (!allowsAnyHost || allowCredentials) {
            call.corsVary()
        }
        
        val origin = call.request.headers.getAll(HttpHeaders.Origin)?.singleOrNull() ?: return
        
        when (checkOrigin(origin, call.request.origin)) {
            OriginCheckResult.OK -> {
            }
            OriginCheckResult.SkipCORS -> return
            OriginCheckResult.Failed -> {
                context.respondCorsFailed()
                return
            }
        }
        
        if (!allowNonSimpleContentTypes) {
            val contentType = call.request.header(HttpHeaders.ContentType)?.let { ContentType.parse(it) }
            if (contentType != null) {
                if (contentType.withoutParameters() !in Configuration.CorsSimpleContentTypes) {
                    context.respondCorsFailed()
                    return
                }
            }
        }
        
        if (call.request.httpMethod == HttpMethod.Options) {
            call.respondPreflight(origin)
            // TODO: it shouldn't be here, because something else can respond to OPTIONS
            // But if noone else responds, we should respond with OK
            context.finish()
            return
        }
        
        if (!call.corsCheckCurrentMethod()) {
            context.respondCorsFailed()
            return
        }
        
        call.accessControlAllowOrigin(origin)
        call.accessControlAllowCredentials()
        
        if (exposedHeaders != null) {
            call.response.header(HttpHeaders.AccessControlExposeHeaders, exposedHeaders)
        }
    }
    
    fun checkOrigin(origin: String, point: RequestConnectionPoint): OriginCheckResult = when {
        !isValidOrigin(origin) -> OriginCheckResult.SkipCORS
        allowSameOrigin && isSameOrigin(origin, point) -> OriginCheckResult.SkipCORS
        !corsCheckOrigins(origin) -> OriginCheckResult.Failed
        else -> OriginCheckResult.OK
    }
    
    private suspend fun ApplicationCall.respondPreflight(origin: String) {
        val requestHeaders =
            request.headers.getAll(HttpHeaders.AccessControlRequestHeaders)?.flatMap { it.split(",") }?.map {
                it.trim().lowercase()
            } ?: emptyList()
        
        if (!corsCheckRequestMethod() || (!corsCheckRequestHeaders(requestHeaders))) {
            respond(HttpStatusCode.Forbidden)
            return
        }
        
        accessControlAllowOrigin(origin)
        accessControlAllowCredentials()
        if (methodsListHeaderValue.isNotEmpty()) {
            response.header(HttpHeaders.AccessControlAllowMethods, methodsListHeaderValue)
        }
        
        val requestHeadersMatchingPrefix = requestHeaders.filter { header -> headerMatchesAPredicate(header) }
        
        val headersListHeaderValue = (headersList + requestHeadersMatchingPrefix).sorted().joinToString(", ")
        
        response.header(HttpHeaders.AccessControlAllowHeaders, headersListHeaderValue)
        accessControlMaxAge()
        
        respond(HttpStatusCode.OK)
    }
    
    private fun ApplicationCall.accessControlAllowOrigin(origin: String) {
        if (allowsAnyHost && !allowCredentials) {
            response.header(HttpHeaders.AccessControlAllowOrigin, "*")
        } else {
            response.header(HttpHeaders.AccessControlAllowOrigin, origin)
        }
    }
    
    private fun ApplicationCall.corsVary() {
        val vary = response.headers[HttpHeaders.Vary]
        if (vary == null) {
            response.header(HttpHeaders.Vary, HttpHeaders.Origin)
        } else {
            response.header(HttpHeaders.Vary, vary + ", " + HttpHeaders.Origin)
        }
    }
    
    private fun ApplicationCall.accessControlAllowCredentials() {
        if (allowCredentials) {
            response.header(HttpHeaders.AccessControlAllowCredentials, "true")
        }
    }
    
    private fun ApplicationCall.accessControlMaxAge() {
        if (maxAgeHeaderValue != null) {
            response.header(HttpHeaders.AccessControlMaxAge, maxAgeHeaderValue)
        }
    }
    
    private fun isSameOrigin(origin: String, point: RequestConnectionPoint): Boolean {
        val requestOrigin = "${point.scheme}://${point.host}:${point.port}"
        return normalizeOrigin(requestOrigin) == normalizeOrigin(origin)
    }
    
    private fun corsCheckOrigins(origin: String): Boolean {
        val normalizedOrigin = normalizeOrigin(origin)
        return allowsAnyHost || normalizedOrigin in hostsNormalized || hostsWithWildcard.any { (prefix, suffix) ->
            normalizedOrigin.startsWith(prefix) && normalizedOrigin.endsWith(suffix)
        }
    }
    
    private fun corsCheckRequestHeaders(requestHeaders: List<String>): Boolean {
        return requestHeaders.all { header ->
            header in allHeadersSet || headerMatchesAPredicate(header)
        }
    }
    
    private fun headerMatchesAPredicate(header: String): Boolean {
        return headerPredicates.any { it(header) }
    }
    
    private fun ApplicationCall.corsCheckCurrentMethod(): Boolean {
        return request.httpMethod in methods
    }
    
    private fun ApplicationCall.corsCheckRequestMethod(): Boolean {
        val requestMethod = request.header(HttpHeaders.AccessControlRequestMethod)?.let { HttpMethod(it) }
        return requestMethod != null && requestMethod in methods
    }
    
    private suspend fun PipelineContext<Unit, ApplicationCall>.respondCorsFailed() {
        call.respond(HttpStatusCode.Forbidden)
        finish()
    }
    
    private fun isValidOrigin(origin: String): Boolean {
        if (origin.isEmpty()) {
            return false
        }
        if (origin == "null") {
            return true
        }
        if ("%" in origin) {
            return false
        }
        
        val protoDelimiter = origin.indexOf("://")
        if (protoDelimiter <= 0) {
            return false
        }
        
        val protoValid = origin[0].isLetter() && origin.subSequence(0, protoDelimiter).all { ch ->
            ch.isLetter() || ch.isDigit() || ch == '-' || ch == '+' || ch == '.'
        }
        
        if (!protoValid) {
            return false
        }
        
        var portIndex = origin.length
        for (index in protoDelimiter + 3 until origin.length) {
            val ch = origin[index]
            if (ch == ':' || ch == '/') {
                portIndex = index + 1
                break
            }
            if (ch == '?') return false
        }
        
        for (index in portIndex until origin.length) {
            if (!origin[index].isDigit()) {
                return false
            }
        }
        
        return true
    }
    
    private fun normalizeOrigin(origin: String) =
        if (origin == "null" || origin == "*") origin else StringBuilder(origin.length).apply {
            append(origin)
            
            if (!origin.substringAfterLast(":", "").matches(numberRegex)) {
                val port = when (origin.substringBefore(':')) {
                    "http" -> "80"
                    "https" -> "443"
                    else -> null
                }
                
                if (port != null) {
                    append(':')
                    append(port)
                }
            }
        }.toString()
    
    /**
     * CORS feature configuration
     */
    class Configuration {
        private val wildcardWithDot = "*."
        
        companion object {
            /**
             * Default HTTP methods that are always allowed by CORS
             */
            val CorsDefaultMethods: Set<HttpMethod> = setOf(HttpMethod.Get, HttpMethod.Post, HttpMethod.Head)
            
            // https://www.w3.org/TR/cors/#simple-header
            /**
             * Default HTTP headers that are always allowed by CORS
             */
            @Suppress("unused")
            @Deprecated(
                "Use CorsSimpleRequestHeaders or CorsSimpleResponseHeaders instead",
                level = DeprecationLevel.ERROR
            )
            val CorsDefaultHeaders: Set<String> = caseInsensitiveSet(
                HttpHeaders.CacheControl,
                HttpHeaders.ContentLanguage,
                HttpHeaders.ContentType,
                HttpHeaders.Expires,
                HttpHeaders.LastModified,
                HttpHeaders.Pragma
            )
            
            /**
             * Default HTTP headers that are always allowed by CORS
             * (simple request headers according to https://www.w3.org/TR/cors/#simple-header )
             * Please note that `Content-Type` header simplicity depends on it's value.
             */
            val CorsSimpleRequestHeaders: Set<String> = caseInsensitiveSet(
                HttpHeaders.Accept,
                HttpHeaders.AcceptLanguage,
                HttpHeaders.ContentLanguage,
                HttpHeaders.ContentType
            )
            
            /**
             * Default HTTP headers that are always allowed by CORS to be used in response
             * (simple request headers according to https://www.w3.org/TR/cors/#simple-header )
             */
            val CorsSimpleResponseHeaders: Set<String> = caseInsensitiveSet(
                HttpHeaders.CacheControl,
                HttpHeaders.ContentLanguage,
                HttpHeaders.ContentType,
                HttpHeaders.Expires,
                HttpHeaders.LastModified,
                HttpHeaders.Pragma
            )
            
            /**
             * The allowed set of content types that are allowed by CORS without preflight check
             */
            @Suppress("unused")
            val CorsSimpleContentTypes: Set<ContentType> =
                @OptIn(InternalAPI::class)
                setOf(
                    ContentType.Application.FormUrlEncoded,
                    ContentType.MultiPart.FormData,
                    ContentType.Text.Plain
                ).unmodifiable()
        }
        
        /**
         * Allowed CORS hosts
         */
        val hosts: MutableSet<String> = HashSet()
        
        /**
         * Allowed CORS headers
         */
        @OptIn(InternalAPI::class)
        val headers: MutableSet<String> = CaseInsensitiveSet()
        
        /**
         * Allowed HTTP methods
         */
        val methods: MutableSet<HttpMethod> = HashSet()
        
        /**
         * Exposed HTTP headers that could be accessed by a client
         */
        @OptIn(InternalAPI::class)
        val exposedHeaders: MutableSet<String> = CaseInsensitiveSet()
        
        /**
         * Allow sending credentials
         */
        var allowCredentials: Boolean = false
        
        /**
         * If present represents the prefix for headers which are permitted in cors requests.
         */
        val headerPredicates: MutableList<(String) -> Boolean> = mutableListOf()
        
        /**
         * Duration in seconds to tell the client to keep the host in a list of known HSTS hosts.
         */
        var maxAgeInSeconds: Long = CORS_DEFAULT_MAX_AGE
            set(newMaxAge) {
                check(newMaxAge >= 0L) { "maxAgeInSeconds shouldn't be negative: $newMaxAge" }
                field = newMaxAge
            }
        
        /**
         * Allow requests from the same origin
         */
        var allowSameOrigin: Boolean = true
        
        /**
         * Allow sending requests with non-simple content-types. The following content types are considered simple:
         * - `text/plain`
         * - `application/x-www-form-urlencoded`
         * - `multipart/form-data`
         */
        var allowNonSimpleContentTypes: Boolean = false
        
        /**
         * Allow requests from any host
         */
        fun anyHost() {
            hosts.add("*")
        }
        
        /**
         * Allow requests from the specified domains and schemes. A wildcard is supported for either the host or any
         * subdomain. If you specify a wildcard in the host, you cannot add specific subdomains. Otherwise you can mix
         * wildcard and non-wildcard subdomains as long as the wildcard is always in front of the domain,
         * e.g. `*.sub.domain.com` but not `sub.*.domain.com`.
         */
        fun host(host: String, schemes: List<String> = listOf("http"), subDomains: List<String> = emptyList()) {
            if (host == "*") {
                return anyHost()
            }
            
            require("://" !in host) { "scheme should be specified as a separate parameter schemes" }
            
            for (schema in schemes) {
                addHost("$schema://$host")
                
                for (subDomain in subDomains) {
                    validateWildcardRequirements(subDomain)
                    addHost("$schema://$subDomain.$host")
                }
            }
        }
        
        private fun addHost(host: String) {
            validateWildcardRequirements(host)
            hosts.add(host)
        }
        
        private fun validateWildcardRequirements(host: String) {
            if ('*' !in host) {
                return
            }
            
            fun String.countMatches(subString: String): Int =
                windowed(subString.length) {
                    if (it == subString) 1 else 0
                }.sum()
            
            require(wildcardInFrontOfDomain(host)) { "wildcard must appear in front of the domain, e.g. *.domain.com" }
            require(host.countMatches(wildcardWithDot) == 1) { "wildcard cannot appear more than once" }
        }
        
        private fun wildcardInFrontOfDomain(host: String): Boolean {
            val indexOfWildcard = host.indexOf(wildcardWithDot)
            return wildcardWithDot in host && !host.endsWith(wildcardWithDot) &&
                    (indexOfWildcard <= 0 || host.substringBefore(wildcardWithDot).endsWith("://"))
        }
        
        /**
         * Allow to expose [header]. It adds the [header] to `Access-Control-Expose-Headers` if it is not a
         * simple response header.
         */
        fun exposeHeader(header: String) {
            if (header !in CorsSimpleResponseHeaders) {
                exposedHeaders.add(header)
            }
        }
        
        /**
         * Allow to expose `X-Http-Method-Override` header
         */
        @Deprecated(
            "Allow it in request headers instead",
            ReplaceWith("allowXHttpMethodOverride()"),
            level = DeprecationLevel.ERROR
        )
        fun exposeXHttpMethodOverride() {
            exposedHeaders.add(HttpHeaders.XHttpMethodOverride)
        }
        
        /**
         * Allow to send `X-Http-Method-Override` header
         */
        @Suppress("unused")
        fun allowXHttpMethodOverride() {
            header(HttpHeaders.XHttpMethodOverride)
        }
        
        /**
         * Allow headers prefixed with [headerPrefix]
         */
        fun allowHeadersPrefixed(headerPrefix: String) {
            this.headerPredicates.add { name -> name.startsWith(headerPrefix) }
        }
        
        /**
         * Allow headers that match [predicate]
         */
        fun allowHeaders(predicate: (String) -> Boolean) {
            this.headerPredicates.add(predicate)
        }
        
        /**
         * Allow sending [header]
         */
        fun header(header: String) {
            if (header.equals(HttpHeaders.ContentType, ignoreCase = true)) {
                allowNonSimpleContentTypes = true
                return
            }
            
            if (header !in CorsSimpleRequestHeaders) {
                headers.add(header)
            }
        }
        
        /**
         * Please note that CORS operates ONLY with REAL HTTP methods
         * and will never consider overridden methods via `X-Http-Method-Override`.
         * However you can add them here if you are implementing CORS at client side from the scratch
         * that you generally don't need to do.
         */
        fun method(method: HttpMethod) {
            if (method !in CorsDefaultMethods) {
                methods.add(method)
            }
        }
    }
    
    /**
     * Feature object for installation
     */
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, BetterCORS> {
        /**
         * The default CORS max age value
         */
        const val CORS_DEFAULT_MAX_AGE: Long = 24L * 3600 // 1 day
        
        override val key: AttributeKey<BetterCORS> = AttributeKey("BetterCORS")
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): BetterCORS {
            val cors = BetterCORS(Configuration().apply(configure))
            pipeline.intercept(ApplicationCallPipeline.Features) { cors.intercept(this) }
            return cors
        }
        
        @OptIn(InternalAPI::class)
        private fun caseInsensitiveSet(vararg elements: String): Set<String> =
            CaseInsensitiveSet(elements.asList())
    }
}

enum class OriginCheckResult {
    OK, SkipCORS, Failed
}
