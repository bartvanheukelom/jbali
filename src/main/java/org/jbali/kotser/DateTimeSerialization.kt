package org.jbali.kotser

import kotlinx.serialization.Serializer
import kotlinx.serialization.modules.SerializersModule
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate

@Serializer(forClass = Instant::class)
object InstantSerializer : StringBasedSerializer<Instant>() {
    override fun fromString(s: String): Instant =
            Instant.parse(s)
}

@Serializer(forClass = LocalDate::class)
object LocalDateSerializer : StringBasedSerializer<LocalDate>() {
    override fun fromString(s: String): LocalDate = LocalDate.parse(s)
}

@Serializer(forClass = Timestamp::class)
object TimestampSerializer : StringBasedSerializer<Timestamp>() {
    override fun fromString(s: String): Timestamp =
            Timestamp.valueOf(s)
}

val dateTimeSerModule = SerializersModule {
    contextual(Instant::class, InstantSerializer)
    contextual(LocalDate::class, LocalDateSerializer)
    contextual(Timestamp::class, TimestampSerializer)
}
