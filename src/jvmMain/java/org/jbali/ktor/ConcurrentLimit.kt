package org.jbali.ktor

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.sync.Semaphore
import org.jbali.text.textable
import org.jbali.text.textableWithCols
import org.jbali.util.NanoDuration
import org.jbali.util.NanoTime
import org.slf4j.LoggerFactory
import org.slf4j.helpers.NOPLogger
import java.net.InetAddress
import java.util.*
import kotlin.time.Duration

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
    
    private data class RequestInfo(
        val uuid: UUID,
        val ip: InetAddress?,
        val method: HttpMethod,
        val path: String,
        /** Time when the request was queued, or null if it was executed immediately */
        val queuedAt: NanoTime? = null,
        /** Time when the request started running, or null if it is still in the queue */
        @Volatile var startedAt: NanoTime? = null,
    ) {
        /** Time when the request was first seen */
        val receivedAt
            get() = queuedAt ?: startedAt ?: NanoTime(Long.MIN_VALUE) // min should never happen
        
        /**
         * Time the request has or had been waiting in the queue, or null if it was executed immediately.
         */
        val waitTime
            get() = queuedAt?.let { NanoDuration.between(it, startedAt ?: NanoTime.now()) }?.toMicro()
        /**
         * Time the request has been running, or null if it is still in the queue.
         */
        val runTime
            get() = startedAt?.let { NanoDuration.between(it, NanoTime.now()) }?.toMicro()
    }
    
    private val stateLock = Any()
    private val activeRequests = mutableMapOf<UUID, RequestInfo>()
    private val queuedRequests = mutableMapOf<UUID, RequestInfo>()
    private var flowState = FlowState.Smooth
    private var lastStateChangeTime: NanoTime = NanoTime(Long.MIN_VALUE)
    private var lastRequestLogTime: NanoTime = NanoTime(Long.MIN_VALUE)
    
    private val activeRequestSemaphore = when (configuration.maxActiveRequests) {
        Int.MAX_VALUE -> null
        else -> Semaphore(configuration.maxActiveRequests)
    }
    
    private val counterRequestsRejectedTotal = configuration.meterRegistry?.let { registry ->
        Counter.builder("ktor.http.requests.rejected")
            .tag("debugName", configuration.debugName ?: "")
            .tag("scope", "global")
            .register(registry)
    }
    private val counterRequestsRejectedForIp = configuration.meterRegistry?.let { registry ->
        Counter.builder("ktor.http.requests.rejected")
            .tag("debugName", configuration.debugName ?: "")
            .tag("scope", "ip")
            .register(registry)
    }
    
    private val meters = configuration.meterRegistry?.let { registry ->
        listOf(
            Gauge.builder("ktor.http.requests.queued") {
                synchronized(stateLock) { queuedRequests.size }
            }
                .tag("debugName", configuration.debugName ?: "")
                .register(registry),
            Gauge.builder("ktor.http.requests.concurrent") {
                synchronized(stateLock) { activeRequests.size }
            }
                .tag("debugName", configuration.debugName ?: "")
                .register(registry),
        )
    }
    
    private fun updateFlowState() {
        assert(Thread.holdsLock(stateLock))
        val now = NanoTime.now()
        
        val oldState = flowState
        val newState = when {
            queuedRequests.size >= configuration.maxQueuedRequests -> FlowState.Blocked
            activeRequests.size >= configuration.maxActiveRequests -> FlowState.Restricted
            else -> FlowState.Smooth
        }
        
        if (configuration.alwaysLogState) {
            data class StateInfo(
                val oldState: FlowState,
                val active: Int,
                val maxActive: Int,
                val queued: Int,
                val maxQueued: Int,
                val newState: FlowState,
            )
            log.info(textable(listOf(StateInfo(
                oldState = oldState,
                active = activeRequests.size,
                maxActive = configuration.maxActiveRequests,
                queued = queuedRequests.size,
                maxQueued = configuration.maxQueuedRequests,
                newState = newState,
            ))).joinToString("\n"))
        }
        
        if (oldState != newState) {
            flowState = newState
            
            val cooldown = configuration.stateChangeCooldown
            if (configuration.alwaysLogState) {
                log.info("cooldown=$cooldown (= ${cooldown?.inWholeNanoseconds} ns), lastStateChangeTime=$lastStateChangeTime, now=$now, timeSinceLastChange=${NanoDuration.between(lastStateChangeTime, now)}")
            }
            if (cooldown == null || NanoDuration.between(lastStateChangeTime, now).ns > cooldown.inWholeNanoseconds) {
                lastStateChangeTime = now
                when {
                    newState.ordinal > oldState.ordinal -> {
                        when (newState) {
                            FlowState.Restricted -> {
                                log.warn("Flow worsened from $oldState to $newState")
                                logActiveRequests()
//                                logAllRequests()
                                lastRequestLogTime = now
                            }
                            FlowState.Blocked -> {
                                log.error("Flow worsened from $oldState to $newState")
//                                logActiveRequests()
//                                logQueuedRequests()
                                logAllRequests()
                                lastRequestLogTime = now
                            }
                            else -> {
                                // unreachable
                                log.info("Flow worsened from $oldState to $newState")
                            }
                        }
                    }
                    else -> log.info("Flow improved from $oldState to $newState")
                }
            }
        }
        
        // sort-of periodic request logging during trouble
        configuration.periodicLoggingInterval?.let { interval ->
            val timeSinceLastPeriodicLog = NanoDuration.between(lastRequestLogTime, now)
            if (timeSinceLastPeriodicLog.ns > interval.inWholeNanoseconds && flowState != FlowState.Smooth) {
                logAllRequests()
                lastRequestLogTime = now
            }
        }
    }
    
    private fun logActiveRequests() {
        log.info("Active requests (${activeRequests.size}):\n${textableWithCols(
            listOf(
                "UUID" to { it.uuid },
                "IP" to { it.ip?.hostAddress ?: "?" }, // '?' because it's unexpected
                "Method" to { it.method.value },
                "Path" to { it.path },
                "Waited" to { it.waitTime ?: "" },
                "Running" to { it.runTime ?: "" },
            ),
            activeRequests.values.sortedBy { it.receivedAt }
        ).joinToString("\n")}")
    }
    private fun logQueuedRequests() {
        log.info("Queued requests (${queuedRequests.size}):\n${textableWithCols(
            listOf(
                "UUID" to { it.uuid },
                "IP" to { it.ip?.hostAddress ?: "?" }, // '?' because it's unexpected
                "Method" to { it.method.value },
                "Path" to { it.path },
                "Waited" to { it.waitTime ?: "" },
            ),
            queuedRequests.values.sortedBy { it.receivedAt }
        ).joinToString("\n")}")
    }
    private fun logAllRequests() {
        log.info("Requests active (${activeRequests.size}), queued (${queuedRequests.size}):\n${textableWithCols(
            listOf(
                "S" to { if (it.startedAt != null) "R" else "Q" },
                "UUID" to { it.uuid },
                "IP" to { it.ip?.hostAddress ?: "?" }, // '?' because it's unexpected
                "Method" to { it.method.value },
                "Path" to { it.path },
                "Waited" to { it.waitTime ?: "" },
                "Running" to { it.runTime ?: "" },
            ),
            (activeRequests.values + queuedRequests.values).sortedBy { it.receivedAt }
        ).joinToString("\n")}")
    }
    
    private suspend fun PipelineContext<Unit, ApplicationCall>.proceedWithLimit() {
        val uuid = call.uuid
        val requestInfo = RequestInfo(
            uuid   = uuid,
            ip     = call.request.origin.remoteIpOrNull,
            method = call.request.httpMethod,
            path   = call.request.path(),
        )
        if (activeRequestSemaphore == null || activeRequestSemaphore.tryAcquire()) {
            synchronized(stateLock) {
                activeRequests[uuid] = requestInfo.copy(startedAt = NanoTime.now())
                updateFlowState()
            }
            try {
                proceed()
            } finally {
                synchronized(stateLock) {
                    activeRequests.remove(uuid)
                    updateFlowState()
                }
                activeRequestSemaphore?.release()
            }
        } else {
            val next: suspend () -> Unit
            synchronized(stateLock) {
                
                val queuedForIp = requestInfo.ip?.let { ip ->
                    queuedRequests.values.count { it.ip == ip }
                } ?: 0
                
                if (queuedRequests.size >= configuration.maxQueuedRequests) {
                    log.warn("Rejecting request $uuid because too many queued requests")
                    counterRequestsRejectedTotal?.increment()
                    next = {
                        configuration.rejectHandler(call)
                        finish()
                    }
                } else if (queuedForIp >= configuration.maxQueuedRequestsPerIp) {
                    log.warn("Rejecting request $uuid from ${requestInfo.ip} " +
                                "because too many queued requests ($queuedForIp) from this IP")
                    counterRequestsRejectedForIp?.increment()
                    next = {
                        configuration.rejectHandler(call)
                        finish()
                    }
                } else {
                    queuedRequests[uuid] = requestInfo.copy(queuedAt = NanoTime.now())
                    updateFlowState()
                    next = {
                        try {
                            activeRequestSemaphore.acquire()
                        } finally {
                            synchronized(stateLock) {
                                queuedRequests.remove(uuid)
                                updateFlowState()
                            }
                        }
                        synchronized(stateLock) {
                            activeRequests[uuid] = requestInfo
                            requestInfo.startedAt = NanoTime.now()
                            updateFlowState()
                        }
                        try {
                            proceed()
                        } finally {
                            synchronized(stateLock) {
                                activeRequests.remove(uuid)
                                updateFlowState()
                            }
                            activeRequestSemaphore.release()
                        }
                    }
                }
            }
            next()
        }
    }
    
    class Configuration {
        var debugName: String? = null
        var alwaysLogState = false
        
        var maxActiveRequests = 32
        
        /**
         * Maximum number of total queued requests. If this limit is reached, new requests will be rejected.
         */
        var maxQueuedRequests = 256
        /**
         * Maximum number of queued requests per IP address. If this limit is reached, new requests from
         * this IP will be rejected.
         */
        var maxQueuedRequestsPerIp = 8
        
        override fun toString(): String {
            return "maxActiveRequests=$maxActiveRequests, maxQueuedRequests=$maxQueuedRequests, maxQueuedRequestsPerIp=$maxQueuedRequestsPerIp"
        }
        
        var meterRegistry: MeterRegistry? = null
        
        var rejectHandler: suspend (ApplicationCall) -> Unit = {
            it.respondBasicError(HttpStatusCode.ServiceUnavailable, "Too many active requests")
        }
        
        fun onReject(handler: suspend (ApplicationCall) -> Unit) {
            rejectHandler = handler
        }
        
        fun noLimit() {
            maxActiveRequests = Int.MAX_VALUE
            maxQueuedRequests = Int.MAX_VALUE
        }
        
        var stateChangeCooldown: Duration? = null
        
        /**
         * Interval for periodic logging of all requests when the state is not Smooth.
         * If null (default), no periodic logging will occur.
         */
        var periodicLoggingInterval: Duration? = null
    }
    
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, ConcurrentLimit> {
        
        private val ConcurrentLimitPreparePhase = PipelinePhase("ConcurrentLimitPrepare")
        
        override val key = AttributeKey<ConcurrentLimit>("ConcurrentLimit")
        
        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): ConcurrentLimit {
            val configuration = Configuration().apply(configure)
            val feature = ConcurrentLimit(configuration)
            val log = NOPLogger.NOP_LOGGER
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

enum class FlowState {
    Smooth, Restricted, Blocked
}