package org.jbali.ktor

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory

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
    
    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, ConcurrentLimit> {
        
        private val ConcurrentLimitPreparePhase = PipelinePhase("ConcurrentLimitPrepare")
        
        override val key = AttributeKey<ConcurrentLimit>("ConcurrentLimit")
        private val LimitsKey = AttributeKey<List<ConcurrentLimit>>("ConcurrentLimit.Limits")
        
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
                call.attributes[LimitsKey] =
                    (call.attributes.getOrNull(LimitsKey) ?: emptyList()) + feature
                trace("limits: ${call.attributes[LimitsKey]}")
                try {
                    proceed()
                } finally {
                    trace("End intercept @ ConcurrentLimitPreparePhase")
                }
            }
            
            pipeline.intercept(ApplicationCallPipeline.Call) {
                val limits = call.attributes[LimitsKey]
                trace("Intercept @ Call, limits: $limits")
                call.attributes.put(LimitsKey, limits.drop(1))
                
                try {
                    if (limits.singleOrNull() != feature) {
                        trace("This limit has been overridden")
                        proceed()
                    } else {
                        trace("We are the last limit! Applying $configuration ...")
                        // TODO actually do the limiting
                        proceed()
                    }
                } finally {
                    trace("End intercept @ Call")
                }
            }
            
            return feature
        }
    }
}
