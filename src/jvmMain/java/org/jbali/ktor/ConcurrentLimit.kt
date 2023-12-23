package org.jbali.ktor

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.sync.Semaphore
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

class ConcurrentLimit private constructor(private val configuration: Configuration) {
    
    private val log = LoggerFactory.getLogger("org.jbali.ktor.ConcurrentLimit[${configuration.debugName}]")
    
    override fun toString() = "ConcurrentLimit[${configuration.debugName}]{${configuration}}"
    
    class Configuration {
        
        var debugName: String? = null
        
        /**
         * The maximum number of concurrent active requests.
         * Extra requests will be queued.
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
        
    }
    
    // semaphore for active requests
    private val activeRequestSemaphore = Semaphore(configuration.maxActiveRequests)
    private val queuedRequests = AtomicInteger(0)
    
    private suspend fun PipelineContext<Unit, ApplicationCall>.proceedWithLimit() {
        if (activeRequestSemaphore.tryAcquire()) {
            try {
                proceed()
            } finally {
                activeRequestSemaphore.release()
            }
        } else {
            if (queuedRequests.incrementAndGet() > configuration.maxQueuedRequests) {
                queuedRequests.decrementAndGet() // TODO I don't like having to undo like this
                configuration.rejectHandler(call)
            } else {
                activeRequestSemaphore.acquire()
                try {
                    proceed()
                } finally {
                    activeRequestSemaphore.release()
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
            val log = feature.log
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
                        feature.log.info("Stack before proceed", RuntimeException())
                        with(feature) { proceedWithLimit() }
                        feature.log.info("Stack after proceed", RuntimeException())
                    }
                } finally {
                    trace("End intercept @ Call")
                }
            }
            
            return feature
        }
    }
}
