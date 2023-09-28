package org.jbali.micrometer

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Timer
import org.jbali.util.NanoDuration
import java.time.Duration
import java.util.concurrent.TimeUnit

fun <T> Gauge.Builder<T>.tags(tags: Map<String, String>): Gauge.Builder<T> {
    tags.forEach(::tag)
    return this
}

fun Timer.Builder.tags(tags: Map<String, String>): Timer.Builder {
    tags.forEach(::tag)
    return this
}

fun Counter.Builder.tags(tags: Map<String, String>): Counter.Builder {
    tags.forEach(::tag)
    return this
}



/**
 * Record an event of the given [duration], in milliseconds.
 * This is a workaround for quickly adding more precision to a Timer whose base unit is seconds -> TODO source?
 * Prefer configuring the Timer with the desired base unit and using [Timer.record] with the [Duration] instead.
 */
fun Timer.recordSneakyMillis(duration: Duration) =
    record(duration.toMillis(), TimeUnit.SECONDS)

fun Timer.record(nd: NanoDuration) =
    record(nd.ns, TimeUnit.NANOSECONDS)
