package org.jbali.kotser.std

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import org.jbali.kotser.StringBasedSerializer
import org.jbali.kotser.Transformer
import org.jbali.kotser.TransformingSerializer
import org.jbali.kotser.transformingSerializer
import org.jbali.threeten.toDate
import org.threeten.extra.Interval
import org.threeten.extra.YearQuarter
import org.threeten.extra.YearWeek
import java.sql.Timestamp
import java.time.*
import java.util.*

/**
 * Serialize as a string in [java.time.format.DateTimeFormatter.ISO_INSTANT].
 */
object InstantSerializer : StringBasedSerializer<Instant>(Instant::class) {
    override fun fromString(s: String): Instant =
            Instant.parse(s)
}

/**
 * Serialize as a Unix timestamp, i.e. seconds since the epoch.
 */
object InstantUnixSerializer : KSerializer<Instant> by transformingSerializer(
    transformer = { it.epochSecond },
    detransformer = { Instant.ofEpochSecond(it) }
)

object DateSerializer : TransformingSerializer<Date, Instant>(Date::class, InstantSerializer) {
    override fun transform(obj: Date): Instant = obj.toInstant()
    override fun detransform(tf: Instant): Date = tf.toDate()
}


/**
 * Serialized form of [Interval].
 */
@Serializable
data class SerializedInterval(
    /**
     * Start of the interval, inclusive.
     * Defaults to [Instant.MIN], which [Interval] considers to be unbounded
     * and thus is good to omit (when omitting defaults is enabled) to enhance
     * compatibility with code that encodes an unbounded start differently.
     */
    val start: @Serializable(with = InstantSerializer::class) Instant = Instant.MIN,
    /**
     * End of the interval, exclusive.
     * Defaults to [Instant.MAX], which [Interval] considers to be unbounded
     * and thus is good to omit (when omitting defaults is enabled) to enhance
     * compatibility with code that encodes an unbounded end differently.
     */
    val end: @Serializable(with = InstantSerializer::class) Instant = Instant.MAX,
)

object IntervalSerializer : KSerializer<Interval> by transformingSerializer(
    transformer = { SerializedInterval(it.start, it.end) },
    detransformer = { Interval.of(it.start, it.end) },
)


object LocalDateSerializer : StringBasedSerializer<LocalDate>(LocalDate::class) {
    override fun fromString(s: String): LocalDate = LocalDate.parse(s)
}

object YearWeekSerializer : StringBasedSerializer<YearWeek>(YearWeek::class) {
    override fun fromString(s: String): YearWeek = YearWeek.parse(s)
}

object YearMonthSerializer : StringBasedSerializer<YearMonth>(YearMonth::class) {
    override fun fromString(s: String): YearMonth = YearMonth.parse(s)
}

object YearQuarterSerializer : StringBasedSerializer<YearQuarter>(YearQuarter::class) {
    override fun fromString(s: String): YearQuarter = YearQuarter.parse(s)
}

object YearSerializer : KSerializer<Year> by transformingSerializer(
        transformer = object : Transformer<Year, Int> {
            override fun transform(obj: Year) = obj.value
            override fun detransform(tf: Int): Year = Year.of(tf)
        }
)

object ZonedDateTimeSerializer : StringBasedSerializer<ZonedDateTime>(ZonedDateTime::class) {
    override fun fromString(s: String): ZonedDateTime = ZonedDateTime.parse(s)
}

object TimestampSerializer : StringBasedSerializer<Timestamp>(Timestamp::class) {
    override fun fromString(s: String): Timestamp =
            Timestamp.valueOf(s)
}

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
