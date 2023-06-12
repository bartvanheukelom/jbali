package org.jbali.jmsrpc

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Metrics
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

internal object TMSMeters {
    
    val countRequestsActive = AtomicInteger(0)
    val gaugeRequestsActive = Gauge.builder("tms_client_requests_active", countRequestsActive::get)
        .description("Number of active TextMessageServiceClient requests")
        .register(Metrics.globalRegistry)
    
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
    
    fun recordClientRequest(ifaceName: String?, methodName: String, success: Boolean, duration: Duration) {
        Metrics.timer(
            "tms_client_requests",
            "iface", ifaceName ?: "null",
            "method", methodName,
            "success", success.toString(),
        ).record(duration)
    }
    
    fun recordIfaceInit(duration: Duration, ifaceName: String?) {
        Metrics.timer(
            "tms_client_iface_init",
            "iface", ifaceName ?: "null",
        ).record(duration)
    }

}
