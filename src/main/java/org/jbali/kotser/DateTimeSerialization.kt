package org.jbali.kotser

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.context.SimpleModule
import java.time.Instant
import java.time.format.DateTimeParseException

@Serializer(forClass = Instant::class)
object InstantSerializer : KSerializer<Instant> {
    override fun deserialize(input: Decoder): Instant =
            input.decodeString().let {
                try {
                    Instant.parse(it)
                } catch (e: DateTimeParseException) {
                    Instant.ofEpochMilli(it.toLong())
                }
            }

    override fun serialize(output: Encoder, obj: Instant) =
            output.encodeString(obj.toString())
}

val dateTimeSerModule = SimpleModule(Instant::class, InstantSerializer)
