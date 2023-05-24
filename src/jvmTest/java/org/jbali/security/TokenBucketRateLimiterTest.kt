package org.jbali.security

import kotlinx.coroutines.*
import org.junit.Test
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.*

class TokenBucketRateLimiterTest {
    
    private val log = LoggerFactory.getLogger(javaClass)
    
    @Test
    fun testRequestPermits() {
        
        var now = Instant.EPOCH
        fun advance(ms: Long) {
            now = now.plusMillis(ms)
            log.info("-------------- ${now.atZone(ZoneOffset.UTC).toLocalTime()} --------------")
        }
        advance(0)
        
        
        val config = TokenBucketRateLimiterConfig(bufferSize = 10u, refillRate = 1.0)
        val rateLimiter = TokenBucketRateLimiter(
            config,
            clock = { now },
            onStateChange = { log.info("state: $it") },
        )
        
        val baseState = rateLimiter.state
        
        // clean state, all permits available
        assertEquals(10u, rateLimiter.getAvailablePermits())
        
        // consume 3 permits
        assertEquals(3u, rateLimiter.requestPermits(permits = 3u))
        assertEquals(7u, rateLimiter.getAvailablePermits())
        
        // request more than available
        assertEquals(0u, rateLimiter.requestPermits(permits = 8u, partial = false))
        assertFailsWith<RateLimitExceededException> {
            rateLimiter.requirePermits(permits = 8u)
        }
        assertEquals(7u, rateLimiter.getAvailablePermits())
        
        // request more than available, but allow partial fulfillment
        assertEquals(7u, rateLimiter.requestPermits(permits = 8u, partial = true))
        assertEquals(0u, rateLimiter.getAvailablePermits())
        
        // request for another key
        assertEquals(10u, rateLimiter.getAvailablePermits("boesboes"))
        assertEquals(4u, rateLimiter.requestPermits("boesboes", permits = 4u))
        assertEquals(6u, rateLimiter.getAvailablePermits("boesboes"))
        
        // refill 1 permit
        assertEquals(0u, rateLimiter.getAvailablePermits())

        advance(333)
        assertEquals(0u, rateLimiter.getAvailablePermits())

        advance(777)
        assertEquals(1u, rateLimiter.getAvailablePermits())
        assertEquals(7u, rateLimiter.getAvailablePermits("boesboes"))

        // 2 more
        advance(2005)
        assertEquals(3u, rateLimiter.getAvailablePermits())
        
        // wait for refill
        advance(10_001)
        
        // state should now equal base state. if so, no need to test again.
        rateLimiter.cleanUpNow(force = true)
        assertEquals(baseState, rateLimiter.state)
        
    }
    
    @Test
    fun testWaitForPermits() {
        val config = TokenBucketRateLimiterConfig(bufferSize = 10u, refillRate = 10.0)
        val rateLimiter = TokenBucketRateLimiter(
            config,
            onStateChange = { log.info("state: $it") },
        )
        
        runBlocking {
            // greedy
            rateLimiter.requirePermits(permits = 10u)
            assertEquals(0u, rateLimiter.getAvailablePermits())
            
            var gotten = false
            val waiter = launch {
                assertEquals(0u, rateLimiter.getAvailablePermits())
                log.info("WAITER: waiting for 1 permit")
                rateLimiter.waitForPermits(permits = 1u)
                log.info("WAITER: got 1 permit")
                gotten = true
            }
            
            // 1 permit should become available after 100ms, but not before
            log.info("T+0.000: assert not gotten")
            assertFalse(gotten)
            delay(60)
            log.info("T+0.060: assert not gotten")
            assertFalse(gotten)
            delay(50)
            log.info("T+0.110: assert gotten")
            assertTrue(gotten)
            
            // taking 3 permits within 100ms should be impossible
            val available = rateLimiter.getAvailablePermits()
            assertFailsWith<TimeoutCancellationException> {
                withTimeout(100) {
                    log.info("waiting for 3 permits")
                    rateLimiter.waitForPermits(permits = 3u)
                    fail("should not have gotten 3 permits")
                }
            }.also { log.info("$it") }
            log.info("T+0.210: timed out")
            
            // as the waiter was cancelled, assert that they are not still consumed as soon as they become available
            delay(300)
            log.info("T+0.510: assert not taken after cancel")
            assertTrue(rateLimiter.getAvailablePermits() >= available + 3u)
            
        }
    }
    
//    @Test
//    fun testCleanUpNow() {
//        var now = Instant.now()
//        val config = TokenBucketRateLimiterConfig(bufferSize = 10u, refillRate = 1.0, cleanupInterval = Duration.ofSeconds(10))
//        val rateLimiter = TokenBucketRateLimiter(config) { now }
//
//        assertEquals(10u, rateLimiter.requestPermits("key1", 10u))
//        assertEquals(10u, rateLimiter.requestPermits("key2", 10u))
//        now = now.plusSeconds(10) // enough time for cleanup interval to pass
//        rateLimiter.cleanUpNow() // should remove "key1" and "key2" from the state
//        assertEquals(0, rateLimiter.state.perKey.size)
//    }
}
