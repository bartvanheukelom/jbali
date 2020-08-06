package org.jbali.rest

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.Parameters
import io.ktor.response.ApplicationResponse
import io.ktor.response.header
import io.ktor.util.flattenEntries
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import org.jbali.json2.KVON
import org.jbali.kotser.KVONDeserializer
import org.jbali.kotser.serializer
import org.jbali.util.ClassedType
import kotlin.reflect.KType


fun ApplicationResponse.addContentTypeInnerHeader(type: KType) {
    header("X-Content-Type-Inner", type.toString().replace(" ", ""))
}

val Parameters.kvon get() =
    KVON.Pairs(flattenEntries())


@OptIn(ImplicitReflectionSerializer::class, ExperimentalStdlibApi::class)
fun <I : Any> PipelineContext<Unit, ApplicationCall>.readInput(type: ClassedType<I>, jsonFormat: Json): I =
        try {
            when (type) {

                ClassedType.unit ->
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
