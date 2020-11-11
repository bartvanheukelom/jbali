package org.jbali.ktor

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HeaderValue
import io.ktor.request.ApplicationRequest
import io.ktor.request.acceptItems
import io.ktor.util.AttributeKey

class Acceptance(
        val items: List<HeaderValue>
) {

    /**
     * Returns the highest quality that is attributed to [ct]
     * by this acceptance, or 0.0 if it's not accepted.
     */
    fun qualityOf(ct: ContentType) =
            items
                    .firstOrNull { ct.match(it.value) }
                    ?.quality
                    ?: 0.0

    /**
     * Returns true if the request accepts `text/html`, and prefers it
     * over `text/plain`. Can be used to e.g. send a fancy or basic default error page.
     *
     * In the following examples, 'x' means '*':
     *
     * - Browser navigation: "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,x/x;q=0.8,application/signed-exchange;v=b3;q=0.9" -> true
     * - Default `curl`: "x/x" -> false
     * - No `Accept` header -> false
     */
    val prefersHtml: Boolean get() =
            qualityOf(ContentType.Text.Html).let {
                it > 0.0 && it > qualityOf(ContentType.Text.Plain)
            }

}

private val akRequestAcceptance = AttributeKey<Acceptance>("requestAcceptance")

val ApplicationRequest.acceptance: Acceptance get() =
        call.attributes.computeIfAbsent(akRequestAcceptance) {
            Acceptance(acceptItems())
        }

val ApplicationCall.prefersHtml: Boolean get() =
        request.acceptance.prefersHtml
