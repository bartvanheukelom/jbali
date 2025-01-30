package org.jbali.jmsrpc

import arrow.core.Either
import io.micrometer.core.instrument.*
import io.opentelemetry.api.common.AttributeKey
import org.jbali.micrometer.CandleGauge
import org.jbali.micrometer.record
import org.jbali.micrometer.recordSneakyMillis
import org.jbali.micrometer.tags
import org.jbali.util.NanoDuration
import org.jbali.util.NanoTime
import org.jbali.util.logger
import org.slf4j.Logger
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

internal object TMSMeters {
    
    private val logMetrics = System.getProperty("org.jbali.jmsrpc.TMSMeters.logMetrics")?.toBoolean() ?: false
    
    private val log = logger<TMSMeters>()
    private val metricsLog: Logger? = log.takeIf { logMetrics }
    
    class ActiveRequestMeter(dir: String) {
        
        private val countActive = AtomicInteger(0)
        
        // TODO look into https://micrometer.io/docs/concepts#_long_task_timers
        private val gaugeActive = Gauge.builder("tms_${dir}_requests_active", countActive::get)
            .description("Number of active TextMessageService $dir requests")
            .register(Metrics.globalRegistry)
        
        private val candleGaugeActive = CandleGauge(
            name = "tms_${dir}_requests_active_hires",
        ) {
            countActive.get().toDouble()
        }
        
        fun increment() {
            countActive.incrementAndGet()
            candleGaugeActive.update()
        }
        
        fun decrement() {
            countActive.decrementAndGet()
            candleGaugeActive.update()
        }
        
        // TODO fun measure(body), replacement for startServerRequest, and also works for client.
        
    }
    
    // TODO should clean up if last client/server is stopped.
    // TODO instead of reference counting that, can also use an alternative for lazy that destroys when not used for a while.
    val activeRequestsClient by lazy { ActiveRequestMeter("client") }
    val activeRequestsServer by lazy { ActiveRequestMeter("server") }
    
//    val countRequests = Counter.builder("tms_client_requests")
//        .description("Rate of TextMessageServiceClient requests")
//        .register(Metrics.globalRegistry)
//    val countResponsesSuccess = Counter.builder("tms_client_responses")
//        .description("Rate of TextMessageServiceClient responses")
//        .tag("success", "true")
//        .register(Metrics.globalRegistry)
//    val countResponsesError = Counter.builder("tms_client_responses")
//        .description("Rate of TextMessageServiceClient error responses")
//        .tag("success", "false")
//        .register(Metrics.globalRegistry)
    
    fun recordStartedServerRequest(ifaceName: String?, methodName: String) {
        Metrics.counter(
            "tms_server_requests_started",
            "iface", ifaceName ?: "null",
            "method", methodName,
        ).increment()
    }
    
    fun recordServerRequest(ifaceName: String?, methodName: String, error: Throwable?, duration: NanoDuration) {
        recordCompletedRequest("server", ifaceName, methodName, error, duration)
    }
    
    fun recordStartedClientRequest(ifaceName: String?, methodName: String) {
        Metrics.counter(
            "tms_client_requests_started",
            "iface", ifaceName ?: "null",
            "method", methodName,
        ).increment()
    }
    
    fun recordClientRequest(ifaceName: String?, methodName: String, error: Throwable?, duration: NanoDuration) {
        recordCompletedRequest("client", ifaceName, methodName, error, duration)
    }
    
    private fun recordCompletedRequest(
        dir: String,
        ifaceName: String?,
        methodName: String,
        error: Throwable?,
        duration: NanoDuration
    ) {
        val metric = "tms_${dir}_requests"
        val tags = mutableMapOf(
            "iface" to (ifaceName ?: "null"),
            "method" to methodName,
            "success" to (error == null).toString(),
        )
        error              ?.let { tags["error1"] = it.javaClass.canonicalName }
        error?.cause       ?.let { tags["error2"] = it.javaClass.canonicalName }
        error?.cause?.cause?.let { tags["error3"] = it.javaClass.canonicalName }
        
        metricsLog?.info("timer($metric, $tags).record($duration)") // TODO disable or make configurable
        Timer.builder(metric)
            .tags(tags)
            .publishPercentileHistogram()
            .register(Metrics.globalRegistry) // this is idempotent, but it looks bad, and is wasteful
            .record(duration)
        
        // custom buckets, log2 based, and non-accumulative
        val cbMetric = "${metric}_cb"
        val bucket = duration.ns // 123'000'555 ns
            .div(1_000_000.0) // 123.000555 ms
            .let(::log2) // ~6.9
            .let(::ceil) // 7.0
            .let { 2.0.pow(it).toInt() } // <= 128 ms
        val cbTags = mapOf(
            "bucket" to bucket.toString(),
        )
        val cbTagsFull = tags + cbTags
        metricsLog?.info("counter(${cbMetric}_count, $cbTagsFull).increment()")
        Counter.builder(cbMetric + "_count")
            .tags(cbTagsFull)
            .register(Metrics.globalRegistry)
            .increment()
        metricsLog?.info("counter(${cbMetric}_ns, $cbTagsFull).increment(${duration.ns.toDouble()})")
        Counter.builder(cbMetric + "_ns")
            .tags(cbTagsFull)
            .register(Metrics.globalRegistry)
            .increment(duration.ns.toDouble())
    }
    
    fun recordIfaceInit(duration: Duration, ifaceName: String?) {
        Metrics.timer(
            "tms_client_iface_init",
            "iface", ifaceName ?: "null",
        ).recordSneakyMillis(duration)
    }
    
    // TODO doesn't need to be nullable
    fun startServerRequest(ifaceName: String?): RequestMeter = object : RequestMeter {
        
        override var methodName: String? = null
            set(value) {
                require(value != null)
                check(field == null) { "methodName already set" }
                recordStartedServerRequest(ifaceName, value)
                ltt = LongTaskTimer.builder("tms_server_requests_active_ltt")
                    .tags(
                        "iface", ifaceName ?: "null",
                        "method", value
                    )
                    .register(Metrics.globalRegistry)
                    .start()
                field = value
            }
        override var result: Either<Throwable, Unit>? = null
        
        private var ltt: LongTaskTimer.Sample? = null
        private var finished = false
        
        init {
            activeRequestsServer.increment()
        }
        
        val ntStart = NanoTime.now()
        
        override fun close() {
            if (!finished) {
                val nd = NanoDuration.since(ntStart)
                finished = true
                ltt?.stop()
                activeRequestsServer.decrement()
                recordServerRequest(
                    ifaceName = ifaceName,
                    methodName = methodName ?: "?",
                    error = when (val r = result) {
                        is Either.Left -> r.value
                        is Either.Right -> null
                        null -> IllegalStateException("result not set")
                    },
                    duration = nd,
                )
            }
        }
    }

}

interface RequestMeter : AutoCloseable {
    var methodName: String?
    var result: Either<Throwable, Unit>?
}

internal object TMSOTelAttributes {
    val iface = AttributeKey.stringKey("tms.iface")
    val method = AttributeKey.stringKey("tms.method")
    
    fun arg(name: String) = argKeys.computeIfAbsent(name) {
        AttributeKey.stringKey("tms.arg.$name")
    }
    private val argKeys = ConcurrentHashMap<String, AttributeKey<String>>()
}
