package org.jbali.micrometer

import io.micrometer.core.instrument.*
import org.jbali.json2.jsonQuote

/**
 * Add the counter to a single registry, or return an existing counter in that
 * registry. The returned counter will be unique for each registry, but each
 * registry is guaranteed to only create one counter for the same combination of
 * name and tags.
 *
 * @param ck the counter key containing the counter name and associated tags.
 * @return A new or existing [Counter].
 */
fun MeterRegistry.counter(ck: CounterKey): Counter =
    Counter.builder(ck.name)
        .tags(ck.mmTags())
        .register(this)

/**
 * Add the timer to a single registry, or return an existing timer in that
 * registry. The returned timer will be unique for each registry, but each
 * registry is guaranteed to only create one timer for the same combination of
 * name and tags.
 *
 * @param tk the timer key containing the timer name and associated tags.
 * @return A new or existing [Timer].
 */
fun MeterRegistry.timer(tk: TimerKey): Timer =
    Timer.builder(tk.name)
        .tags(tk.mmTags())
        .register(this)

/**
 * Add the gauge to a single registry, or return an existing gauge in that
 * registry. The returned gauge will be unique for each registry, but each
 * registry is guaranteed to only create one gauge for the same combination of
 * name and tags.
 *
 * @param gk the gauge key containing the gauge name and associated tags.
 * @param valueFunction a function that returns the value to be monitored.
 * @return A new or existing [Gauge].
 */
inline fun MeterRegistry.gauge(gk: GaugeKey, crossinline valueFunction: () -> Double): Gauge =
    Gauge.builder(gk.name, null) { valueFunction() }
        .tags(gk.mmTags())
        .register(this)

/**
 * Represents a meter key consisting of a name and a set of tags.
 *
 * This key is used to uniquely identify and create [Meter]s in a type-safe manner.
 *
 * @param name the name of the meter.
 * @param tags a map of tag names to tag values.
 */
data class MeterKey<M : Meter>(
    val name: String,
    val tags: Map<String, String>,
) {
    override fun toString(): String =
        buildList {
            add("__name__" to name)
            tags.forEach { (k, v) -> add(k to v) }
        }.joinToString(
            prefix = "{",
            separator = ", ",
            postfix = "}",
        ) {
            "${it.first}=${it.second.jsonQuote()}"
        }
    
    /**
     * Converts the internal tag map into a Micrometer [Tags] object.
     *
     * @return the tags in a format suitable for Micrometer.
     */
    fun mmTags(): Tags =
        tags.map { (k, v) -> Tag.of(k, v) }
            .let { Tags.of(it) }
}

/**
 * Returns a typed string representation of the meter key, prefixed with the meter type.
 *
 * This is useful for logging or debugging, to distinguish keys by their intended meter type.
 *
 * @return a string representation including the simple name of the meter type.
 */
inline fun <reified M : Meter> MeterKey<M>.toTypedString(): String =
    "${M::class.simpleName}${this}"

/**
 * Type alias for a meter key representing a [Counter].
 */
typealias CounterKey = MeterKey<Counter>

/**
 * Type alias for a meter key representing a [Timer].
 */
typealias TimerKey = MeterKey<Timer>

/**
 * Type alias for a meter key representing a [Gauge].
 */
typealias GaugeKey = MeterKey<Gauge>
