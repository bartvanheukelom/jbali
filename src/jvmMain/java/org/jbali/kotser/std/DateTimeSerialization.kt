package org.jbali.kotser.std

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.modules.SerializersModule
import org.jbali.kotser.StringBasedSerializer
import org.jbali.kotser.Transformer
import org.jbali.kotser.TransformingSerializer
import org.jbali.kotser.transformingSerializer
import org.jbali.threeten.toDate
import org.threeten.extra.YearQuarter
import org.threeten.extra.YearWeek
import java.sql.Timestamp
import java.time.*
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

@Serializer(forClass = YearWeek::class)
object YearWeekSerializer : StringBasedSerializer<YearWeek>(YearWeek::class) {
    override fun fromString(s: String): YearWeek = YearWeek.parse(s)
}

@Serializer(forClass = YearMonth::class)
object YearMonthSerializer : StringBasedSerializer<YearMonth>(YearMonth::class) {
    override fun fromString(s: String): YearMonth = YearMonth.parse(s)
}

@Serializer(forClass = YearQuarter::class)
object YearQuarterSerializer : StringBasedSerializer<YearQuarter>(YearQuarter::class) {
    override fun fromString(s: String): YearQuarter = YearQuarter.parse(s)
}

object YearSerializer : KSerializer<Year> by transformingSerializer(
        transformer = object : Transformer<Year, Int> {
            override fun transform(obj: Year) = obj.value
            override fun detransform(tf: Int): Year = Year.of(tf)
        }
)

@Serializer(forClass = ZonedDateTime::class)
object ZonedDateTimeSerializer : StringBasedSerializer<ZonedDateTime>(ZonedDateTime::class) {
    override fun fromString(s: String): ZonedDateTime = ZonedDateTime.parse(s)
}

@Serializer(forClass = Timestamp::class)
object TimestampSerializer : StringBasedSerializer<Timestamp>(Timestamp::class) {
    override fun fromString(s: String): Timestamp =
            Timestamp.valueOf(s)
}

@Serializer(forClass = Duration::class)
object DurationSerializer : StringBasedSerializer<Duration>(Duration::class) {
    override fun fromString(s: String): Duration =
            Duration.parse(s)
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
