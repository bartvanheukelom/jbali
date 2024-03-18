package org.jbali.ktor

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.sync.Semaphore
import org.slf4j.LoggerFactory
import org.slf4j.helpers.NOPLogger
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Feature for limiting the number of concurrent requests. If the limit is reached, incoming requests will be queued
 * until they can be processed. If the queue is full they will be rejected.
 *
 * This feature can be installed at application level or at route level. The limits of the most specific route will
 * be applied. For example, if you limit `/foo` to 10 requests and `/foo/bar` to 20, then the application
 * will allow 20 concurrent requests to `/foo/bar`, as well as 10 concurrent requests to `/foo/bro` at the same time.
 *
 * Can export MicroMeter gauges for queued and active requests, and a counter for rejected requests.
 */
class ConcurrentLimit private constructor(private val configuration: Configuration) {
    
    private val log = LoggerFactory.getLogger("org.jbali.ktor.ConcurrentLimit[${configuration.debugName}]")
    
    override fun toString() = "ConcurrentLimit[${configuration.debugName}]{${configuration}}"
    
    class Configuration {
        
        var debugName: String? = null
        
        /**
         * The maximum number of concurrent active requests.
         * Extra requests will be queued.
         * If set to Int.MAX_VALUE, there will be no limit. This can be used to undo the limit
         * imposed by a parent route.
         */
        var maxActiveRequests = 32
        
        /**
         * The maximum number of queued requests.
         * Extra requests will be rejected by [rejectHandler].
         */
        var maxQueuedRequests = 256
        
        override fun toString(): String {
            return "maxActiveRequests=$maxActiveRequests, maxQueuedRequests=$maxQueuedRequests"
        }
        
        /**
         * If set, meters will be registered with this registry.
         * The configured [debugName] will be added as tag to all metrics.
         */
        var meterRegistry: MeterRegistry? = null
        
        /**
         * Handler for rejected requests. By default, returns a simple error page with HTTP [503 Service Unavailable](HttpStatusCode.ServiceUnavailable).
         */
        var rejectHandler: suspend (ApplicationCall) -> Unit = {
            it.respondBasicError(HttpStatusCode.ServiceUnavailable, "Too many active requests")
        }
        
        /**
         * Syntactic sugar for setting [rejectHandler].
         */
        fun onReject(handler: suspend (ApplicationCall) -> Unit) {
            rejectHandler = handler
        }
        
        fun noLimit() {
            maxActiveRequests = Int.MAX_VALUE
            maxQueuedRequests = Int.MAX_VALUE // not used but set anyway
        }
        
    }
    
    private val activeRequestSemaphore = when (configuration.maxActiveRequests) {
        Int.MAX_VALUE -> null
        else -> Semaphore(configuration.maxActiveRequests)
    }
    private val activeRequests = AtomicInteger(0)
    private val queuedRequests = AtomicInteger(0)
    
    private val counterRequestsRejected = configuration.meterRegistry?.let { registry ->
        Counter.builder("ktor.http.requests.rejected")
            .tag("debugName", configuration.debugName ?: "")
            .register(registry)
    }
    private val meters = configuration.meterRegistry?.let { registry ->
        listOf(
            Gauge.builder("ktor.http.requests.queued", queuedRequests::get)
                .tag("debugName", configuration.debugName ?: "")
                .register(registry),
            Gauge.builder("ktor.http.requests.concurrent", activeRequests::get)
                .tag("debugName", configuration.debugName ?: "")
                .register(registry),
        )
    }
    // TODO clean them up
    
    private val seq = AtomicLong(0)
    
    private suspend fun PipelineContext<Unit, ApplicationCall>.proceedCounted() {
        activeRequests.incrementAndGet()
        try {
            proceed()
        } finally {
            activeRequests.decrementAndGet()
        }
    }
    
    private suspend fun PipelineContext<Unit, ApplicationCall>.proceedWithLimit() {
        if (activeRequestSemaphore == null) {
            proceedCounted()
        } else {
            val rs = seq.incrementAndGet()
            val rid = "${call.uuid}#${rs}"
            if (activeRequestSemaphore.tryAcquire()) {
                try {
//                    log.info("QQQ immediate proceed $rid")
                    proceedCounted()
                } finally {
                    activeRequestSemaphore.release()
                }
            } else {
                val qr = queuedRequests.incrementAndGet()
                if (qr > configuration.maxQueuedRequests || qr < 0) { // < 0 by overflow
                    queuedRequests.decrementAndGet() // TODO I don't like having to undo like this
                    log.warn("Rejecting request $rid because too many queued requests")
                    counterRequestsRejected?.increment()
                    configuration.rejectHandler(call)
                    finish()
                } else {
                    log.warn("Queueing request $rid because too many active requests. Queued requests: $qr")
                    try {
                        activeRequestSemaphore.acquire()
                    } finally {
                        queuedRequests.decrementAndGet()
                    }
                    try {
                        log.info("Proceeding with queued request $rid")
                        proceedCounted()
                    } finally {
                        activeRequestSemaphore.release()
                    }
                }
            }
        }
    }
    
    
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, ConcurrentLimit> {
        
        private val ConcurrentLimitPreparePhase = PipelinePhase("ConcurrentLimitPrepare")
        
        override val key = AttributeKey<ConcurrentLimit>("ConcurrentLimit")
        
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): ConcurrentLimit {
            val configuration = Configuration().apply(configure)
            val feature = ConcurrentLimit(configuration)
            val log = NOPLogger.NOP_LOGGER // feature.log
            log.info("Installing")
            
            fun PipelineContext<*, ApplicationCall>.trace(msg: String) {
                log.info("{call=${System.identityHashCode(call)}} $msg")
            }
            
            pipeline.insertPhaseBefore(ApplicationCallPipeline.Call, ConcurrentLimitPreparePhase)
            
            pipeline.intercept(ConcurrentLimitPreparePhase) {
                trace("Intercept @ ConcurrentLimitPreparePhase")
                call.attributes[key] = feature
                trace("limit: ${call.attributes[key]}")
                try {
                    proceed()
                } finally {
                    trace("End intercept @ ConcurrentLimitPreparePhase")
                }
            }
            
            pipeline.intercept(ApplicationCallPipeline.Call) {
                val limit = call.attributes[key]
                trace("Intercept @ Call, limit: $limit")
                
                try {
                    if (limit != feature) {
                        trace("This limit has been overridden")
                        proceed()
                    } else {
                        trace("We are the last limit! Applying $configuration ...")
                        log.info("Stack before proceed", RuntimeException())
                        with(feature) { proceedWithLimit() }
                        log.info("Stack after proceed", RuntimeException())
                    }
                } finally {
                    trace("End intercept @ Call")
                }
            }
            
            return feature
        }
    }
}
