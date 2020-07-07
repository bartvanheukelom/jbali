package org.jbali.kotser.std

import kotlinx.serialization.Serializer
import kotlinx.serialization.modules.SerializersModule
import org.jbali.kotser.StringBasedSerializer
import org.jbali.kotser.TransformingSerializer
import org.jbali.threeten.toDate
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Serializer(forClass = Instant::class)
object InstantSerializer : StringBasedSerializer<Instant>(Instant::class) {
    override fun fromString(s: String): Instant =
            Instant.parse(s)
}

object DateSerializer : TransformingSerializer<Date, Instant>(Date::class, InstantSerializer) {
    override fun transform(obj: Date): Instant = obj.toInstant()
    override fun detransform(tf: Instant): Date = tf.toDate()
}

@Serializer(forClass = LocalDate::class)
object LocalDateSerializer : StringBasedSerializer<LocalDate>(LocalDate::class) {
    override fun fromString(s: String): LocalDate = LocalDate.parse(s)
}

@Serializer(forClass = Timestamp::class)
object TimestampSerializer : StringBasedSerializer<Timestamp>(Timestamp::class) {
    override fun fromString(s: String): Timestamp =
            Timestamp.valueOf(s)
}

/**
 * Serializer module containing contextual implementations
 * for some JSR-310 (ThreeTen) date/time classes.
 */
val dateTimeSerModule = SerializersModule {
    contextual(Instant::class, InstantSerializer)
    contextual(Date::class, DateSerializer)
    contextual(LocalDate::class, LocalDateSerializer)
    contextual(Timestamp::class, TimestampSerializer)
}
