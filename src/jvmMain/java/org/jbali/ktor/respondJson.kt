package org.jbali.ktor

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jbali.kotser.BasicJson


/**
 * Respond with the given [json] body. Sets the [ContentType] to [ContentType.Application.Json] with [Charsets.UTF_8].
 *
 * @param encoder Format to use for stringifying the [json]. Defaults to [BasicJson.indented].
 * @param status The [HttpStatusCode] of the response. If not set, uses the current value in the [ApplicationResponse],
 * which defaults to [HttpStatusCode.OK].
 */
suspend fun ApplicationCall.respondJson(
    json: JsonElement,
    status: HttpStatusCode? = null,
    encoder: Json = BasicJson.indented
) =
    respond(json.toHTTPContent(status = status, encoder = encoder))


/**
 * Convert this [JsonElement] to a [TextContent], with [ContentType.Application.Json] using [Charsets.UTF_8].
 *
 * @param encoder Format to use for stringifying the JSON. Defaults to [BasicJson.indented].
 * @param status Will be the [OutgoingContent.status]. Defaults to `null`.
 */
fun JsonElement.toHTTPContent(
    status: HttpStatusCode? = null,
    encoder: Json = BasicJson.indented
) =
    TextContent(
        text = encoder.encodeToString(this),
        contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8),
        status = status
    )
