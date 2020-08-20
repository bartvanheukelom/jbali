package org.jbali.ktor

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.WriterContent
import io.ktor.response.defaultTextContentType
import io.ktor.response.respond
import io.ktor.utils.io.ByteWriteChannel
import java.io.PrintWriter

/**
 * Represents a content that is product by [body] function,
 * using the [PrintWriter] API.
 */
class PrintWriterContent(
        override val contentType: ContentType = ContentType.Text.Plain,
        override val status: HttpStatusCode? = null,
        private val body: suspend PrintWriter.() -> Unit
) : OutgoingContent.WriteChannelContent() {

    override suspend fun writeTo(channel: ByteWriteChannel) {
        WriterContent(
                body = {
                    body(PrintWriter(this))
                },
                contentType = contentType,
                status = status
        ).writeTo(channel)
    }
}

/**
 * Respond with text content [PrintWriter].
 *
 * The [printer] parameter will be called later when engine is ready to produce content.
 * Provided [PrintWriter] will be closed automatically.
 */
suspend fun ApplicationCall.respondTextPrinter(contentType: ContentType? = null, status: HttpStatusCode? = null, printer: suspend PrintWriter.() -> Unit) {
    respond(PrintWriterContent(
            status = status,
            contentType = defaultTextContentType(contentType),
            body = printer
    ))
}
