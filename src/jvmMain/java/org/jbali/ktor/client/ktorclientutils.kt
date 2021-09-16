package org.jbali.ktor.client

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.json.JsonElement
import org.jbali.ktor.toHTTPContent


fun HttpRequestBuilder.json(jsonBody: JsonElement) {
    body = jsonBody.toHTTPContent()
}

fun HttpRequestData.requestString() =
    buildString { 
        append(method.value)
        append(" ")
        append(url.fullPath)
        appendLine()
        
        headers.flattenForEach { h, v ->
            append(h)
            append(": ")
            append(v)
            appendLine()
        }
        
        appendLine()
        appendLine(body)
    }
