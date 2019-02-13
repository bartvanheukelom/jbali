package org.jbali.kotser

import kotlinx.serialization.Serializer
import kotlinx.serialization.context.SimpleModule
import java.time.Instant
import java.time.format.DateTimeParseException

@Serializer(forClass = Instant::class)
object InstantSerializer : StringBasedSerializer<Instant>() {
    override fun fromString(s: String): Instant =
            try {
                Instant.parse(s)
            } catch (e: DateTimeParseException) {
                Instant.ofEpochMilli(s.toLong())
            }
}

val dateTimeSerModule = SimpleModule(Instant::class, InstantSerializer)
