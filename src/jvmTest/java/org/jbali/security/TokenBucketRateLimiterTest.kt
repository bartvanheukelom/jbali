package org.jbali.security

import org.junit.Test
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals

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
    
//    @Test
//    fun testRequirePermits() {
//        var now = Instant.now()
//        val config = TokenBucketRateLimiterConfig(bufferSize = 10u, refillRate = 1.0)
//        val rateLimiter = TokenBucketRateLimiter(config, clock = { now })
//
//        rateLimiter.requirePermits(permits = 5u)
//        assertFailsWith<RuntimeException> { rateLimiter.requirePermits(permits = 6u) }
//    }
    
//    @Test
//    fun testWaitForPermits() {
////        var now = Instant.now()
//        val config = TokenBucketRateLimiterConfig(bufferSize = 10u, refillRate = 1.0)
//        val rateLimiter = TokenBucketRateLimiter(config)
//
//        runBlocking {
//            rateLimiter.waitForPermits(permits = 5u)
////            now = now.plusSeconds(10)  // simulate time passing
//            rateLimiter.waitForPermits(permits = 5u) // should not block because enough time has passed for refill
//        }
//    }
    
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
