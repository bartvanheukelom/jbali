package org.jbali.ktor

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.HttpStatusCodeContent
import io.ktor.http.content.TextContent
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.response.respondText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jbali.kotser.BasicJson
import org.jbali.kotser.stringify

suspend fun ApplicationCall.respondJpeg(data: ByteArray) =
        respondBytes(
                contentType = ContentType.Image.JPEG,
                bytes = data
        )

suspend fun ApplicationCall.respondJson(
        json: JsonElement,
        status: HttpStatusCode? = null,
        encoder: Json = BasicJson.indented
) =
        respond(json.toHTTPContent(status = status, encoder = encoder))

fun JsonElement.toHTTPContent(
        status: HttpStatusCode? = null,
        encoder: Json = BasicJson.indented
) =
        TextContent(
                text = encoder.stringify(this),
                contentType = ContentType.Application.Json,
                status = status
        )

suspend fun ApplicationCall.respondNoContent() =
        respond(HttpStatusCodeContent(HttpStatusCode.NoContent))

// --------------------- basic errors ------------------- //

// TODO this solution doesn't feel clean, but it's acceptable for now
class BasicErrorException(
        val status: HttpStatusCode,
        message: String,
        cause: Throwable? = null
) : RuntimeException(cause?.let { "$message: $it" } ?: message, cause)

suspend fun ApplicationCall.respond(bee: BasicErrorException) =
        respondBasicError(
                status = bee.status,
                message = bee.message ?: ""
        )

suspend fun ApplicationCall.respondBasicError(
        status: HttpStatusCode,
        message: String
) =
        respondText(
                status = status,
                text = "Error $status - $message"
        )

suspend fun ApplicationCall.respondBasicNotFound(
        message: String
) =
        respondBasicError(
                status = HttpStatusCode.NotFound,
                message = message
        )