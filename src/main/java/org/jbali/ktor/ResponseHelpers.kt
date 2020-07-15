package org.jbali.ktor

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondBytes
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import java.io.PrintWriter


/**
 * Respond with text content [PrintWriter].
 *
 * The [writer] parameter will be called later when engine is ready to produce content.
 * Provided [PrintWriter] will be closed automatically.
 */
suspend fun ApplicationCall.respondTextPrinter(contentType: ContentType? = null, status: HttpStatusCode? = null, writer: suspend PrintWriter.() -> Unit) {
    respondTextWriter(
            contentType = contentType,
            status = status
    ) {
        PrintWriter(this).writer()
    }
}

suspend fun ApplicationCall.respondJpeg(data: ByteArray) =
        respondBytes(
                contentType = ContentType.Image.JPEG,
                bytes = data
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