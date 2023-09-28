package org.jbali.micrometer

import com.google.common.util.concurrent.AtomicDouble
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Gauge.Builder
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import kotlinx.coroutines.*
import org.jbali.coroutines.runDelayLoop

class CandleGauge(
    val name: String,
    private val registry: MeterRegistry = Metrics.globalRegistry,
    val tags: Map<String, String> = emptyMap(),
    val pollIntervalMs: Long = 1000,
    val value: () -> Double,
) : AutoCloseable {
    
    @Volatile private var minValue = Double.NaN
    @Volatile private var maxValue = Double.NaN
    
    @Suppress("MoveLambdaOutsideParentheses")
    private val gauges = listOf(
        
        // TODO use another API so we can handle a single scrape of multiple gauges atomically.
        // TODO locking.
        
        Gauge.builder("${name}_min", {
            if (minValue.isNaN()) {
                update()
            }
            val v = minValue
            minValue = Double.NaN
            v
        })
            .tags(tags)
            .register(registry),
        
        Gauge.builder("${name}_max", {
            if (maxValue.isNaN()) {
                update()
            }
            val v = maxValue
            maxValue = Double.NaN
            v
        })
            .tags(tags)
            .register(registry),
        
    )
    
    @OptIn(DelicateCoroutinesApi::class)
    private val loop = GlobalScope.launch {
        runDelayLoop(pollIntervalMs) {
            update()
        }
    }
    
    fun update() {
        val value = value()
        if (minValue.isNaN() || value < minValue) minValue = value
        if (maxValue.isNaN() || value > maxValue) maxValue = value
    }
    
    override fun close() {
        runBlocking {
            loop.cancelAndJoin()
        }
        gauges.forEach(registry::remove)
        gauges.forEach(Gauge::close)
    }
    
}
