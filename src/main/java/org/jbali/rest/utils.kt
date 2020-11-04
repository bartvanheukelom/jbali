package org.jbali.rest

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.json.Json
import org.jbali.json2.KVON
import org.jbali.kotser.KVONDeserializer
import org.jbali.kotser.serializer
import org.jbali.util.ReifiedType
import kotlin.reflect.KType

/**
 * Check whether this content type is a valid JSON type.
 *
 * Does not catch all invalid cases. For example, `application/.vnd.dotty...+json` is allowed.
 * @throws IllegalArgumentException if this content type is not a valid JSON type.
 */
fun ContentType.requireJson() {
    require(contentType == ContentType.Application.Json.contentType)

    val jsonSub = ContentType.Application.Json.contentSubtype
    if (contentSubtype != jsonSub) {
        require(jsonSub in contentSubtype.split('+'))
    }
}

fun ApplicationResponse.addContentTypeInnerHeader(type: KType) {
    header("X-Content-Type-Inner", type.toString().replace(" ", ""))
}

val Parameters.kvon get() =
    KVON.Pairs(flattenEntries())


fun <I : Any> PipelineContext<Unit, ApplicationCall>.readInput(type: ReifiedType<I>, jsonFormat: Json): I =
        try {
            when (type) {

                ReifiedType.unit ->
                    @Suppress("UNCHECKED_CAST")
                    Unit as I

                else -> {
                    // TODO construct once, when declaring input-taking path
                    val deser = KVONDeserializer(
                            deserializer = type.serializer,
                            jsonFormat = jsonFormat
                    )

                    call.parameters
                            .kvon
                            .let(deser::deserialize)
                }

            }
        } catch (e: Throwable) {
            throw IllegalArgumentException("Error constructing input of type $type from request: $e", e)
        }
