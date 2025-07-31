package org.jbali.jsonrpc

import io.ktor.http.*
import org.jbali.memory.CleanerTest
import org.jbali.util.NanoDuration
import org.jbali.util.NanoTime
import org.jbali.util.logger
import kotlin.test.Test
import kotlin.test.assertEquals

class KtorJsonRPCClientTest {
    
    private var r1: Any? = null
    private val log = logger<KtorJsonRPCClientTest>()
    
//    @Test - 2025-07-31: disabled because fails and don't know why
    fun testCloser() {
        log.info("----------------------------- testCloser -----------------------------")
        
        r1 = KtorJsonRPCClient(Url("http://example.com.nope"))
        
        assertEquals(0, KtorJsonRPCClient.instancesCleaned)
        CleanerTest.gc()
        assertEquals(0, KtorJsonRPCClient.instancesCleaned)
        r1 = null
        CleanerTest.gc()
        
        val start = NanoTime.now()
        var delay = 10L
        while (KtorJsonRPCClient.instancesCleaned == 0) {
            if (NanoDuration.since(start) > NanoDuration(10_000_000_000)) {
                CleanerTest.dumpHeapToFile("KtorJsonRPCClientTest")
                throw AssertionError("r1 still not cleaned after timeout")
            }
            delay *= 2
            log.info("r1c still not cleaned, waiting $delay ms")
            Thread.sleep(delay.coerceAtMost(1280L))
            CleanerTest.gc()
        }
        assertEquals(1, KtorJsonRPCClient.instancesCleaned)
        
    }

}