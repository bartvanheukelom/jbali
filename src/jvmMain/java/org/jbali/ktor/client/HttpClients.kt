package org.jbali.ktor.client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.micrometer.core.instrument.Metrics
import kotlinx.coroutines.Job
import org.jbali.util.logger
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object HttpClients {
    
    private val log = logger<HttpClients>()
    
    private val clientIds = AtomicInteger(0)
    private val threadCaptureLock = ReentrantLock()
    
//    fun default() = create()
    
    /**
     * Creates a new HTTP client with a dedicated CIO engine. It must be closed after use, or it will leak threads!
     *
     * Attempts to rename the threads created by the CIO dispatcher from "ktor-cio-dispatcher-worker-$n"
     * to "httpclient-$id-$usage-$n", but this may give incorrect results if a CIO engine is created concurrently
     * without going through [HttpClients].
     * TODO do newer versions of Ktor have a better way to do this? if not, feature request
     *
     * @param usage A string describing the usage type of this client, for logging and metrics. E.g. "default", "slackmsg".
     * @param onClosed A callback that's invoked after the client is closed. Primarily present for testing the implementation.
     * @param config A block that configures the client. Defaults to no special configuration.
     */
    @Deprecated("avoid creating new clients, it seems they leak threads even when closed. under investigation.")
    fun create(
        usage: String = "default",
        onClosed: (() -> Unit)? = null,
        config: HttpClientConfig<CIOEngineConfig>.() -> Unit = {},
    ): HttpClient = threadCaptureLock.withLock {
        val id = clientIds.incrementAndGet()
        log.info("Creating HTTP client #$id for $usage")
        
        val threadsBefore: List<Thread> = Thread.getAllStackTraces().keys.toList() // TODO more efficient way to get this? don't need stack traces
        val client = HttpClient(CIO, config)
        Thread.sleep(10) // TODO horrible! TEMP quick hack to capture more of the threads
        val threadsAfter: List<Thread> = Thread.getAllStackTraces().keys.toList()
        
        val threadIdsBefore = threadsBefore.mapTo(mutableSetOf()) { it.id }
        
        // TODO this is very hacky as it assumes all clients are created through here
        for (thread in threadsAfter) {
            if (thread.id !in threadIdsBefore && thread.name.startsWith("ktor-cio-dispatcher-worker-")) {
                val nn = thread.name.replace("ktor-cio-dispatcher-worker-", "httpclient-$id-$usage-")
                log.info("Renaming thread #${thread.id} ${thread.name} -> $nn")
                thread.name = nn
            }
        }
        
        client.coroutineContext[Job]!!.invokeOnCompletion {
            log.info("HTTP client #$id for $usage was closed")
            Metrics.counter("httpclient.closed", "usage", usage).increment()
            onClosed?.invoke()
        }
        
        Metrics.counter("httpclient.created", "usage", usage).increment()
        
        client
        
    }
    
}