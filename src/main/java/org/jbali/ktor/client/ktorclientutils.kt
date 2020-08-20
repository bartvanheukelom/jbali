package org.jbali.ktor.client

import io.ktor.client.request.HttpRequestBuilder
import kotlinx.serialization.json.JsonElement
import org.jbali.ktor.toHTTPContent


fun HttpRequestBuilder.json(jsonBody: JsonElement) {
    body = jsonBody.toHTTPContent()
}
