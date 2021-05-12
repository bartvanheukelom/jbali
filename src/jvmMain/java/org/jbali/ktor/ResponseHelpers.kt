package org.jbali.ktor

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*

suspend fun ApplicationCall.respondJpeg(data: ByteArray) =
    respondBytes(
        contentType = ContentType.Image.JPEG,
        bytes = data
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
        text = "Error $status\n---------\n$message\n"
    )

suspend fun ApplicationCall.respondBasicNotFound(
    message: String
) =
    respondBasicError(
        status = HttpStatusCode.NotFound,
        message = message
    )