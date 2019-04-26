package org.jbali.kotser

import kotlinx.serialization.Serializer
import kotlinx.serialization.modules.SerializersModule
import java.time.Instant
import java.time.LocalDate
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

@Serializer(forClass = LocalDate::class)
object LocalDateSerializer : StringBasedSerializer<LocalDate>() {
    override fun fromString(s: String): LocalDate = LocalDate.parse(s)
}

val dateTimeSerModule = SerializersModule {
    contextual(Instant::class, InstantSerializer)
    contextual(LocalDate::class, LocalDateSerializer)
}
