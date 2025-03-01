package org.jbali.security

import org.junit.Test
import java.net.InetAddress
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith



class MultiRateLimiterTest {
    
    data class Oppy(
        val method: String,
        val path: String,
        val ip: InetAddress,
        val user: Long,
    )
    
    // A helper factory to build a new rate limiter with fixed rules and a mutable clock.
    private fun createLimiter(clock: () -> Instant): MultiRateLimiter<Oppy> {
        return MultiRateLimiter(
            rules = listOf(
                MultiRateLimiter.Rule(
                    name = "api",
                    scope = { it.path == "/api" || it.path.startsWith("/api/") },
                    groupings = listOf(
                        MultiRateLimiter.Grouping(
                            name = "global",
                            opGroup = { Unit }, // all operations share the same global key
                            rates = listOf(
                                BurstRate(3u, Duration.ofSeconds(1)),
                                BurstRate(60u, Duration.ofMinutes(1))
                            )
                        ),
                        MultiRateLimiter.Grouping(
                            name = "ip",
                            opGroup = { it.ip },
                            rates = listOf(
                                BurstRate(10u, Duration.ofSeconds(1))
                            )
                        )
                    )
                ),
                MultiRateLimiter.Rule(
                    name = "api/foobars",
                    scope = { it.path == "/api/foobars" },
                    groupings = listOf(
                        MultiRateLimiter.Grouping(
                            name = "global",
                            opGroup = { Unit },
                            rates = listOf(
                                BurstRate(1u, Duration.ofSeconds(1))
                            )
                        ),
                        MultiRateLimiter.Grouping(
                            name = "user",
                            opGroup = { it.user },
                            rates = listOf(
                                BurstRate(5u, Duration.ofSeconds(1))
                            )
                        )
                    )
                )
            ),
            clock = clock
        )
    }
    
    @Test
    fun testGlobalApiLimit() {
        // Set up a mutable clock.
        var now: Instant = Instant.parse("2021-01-01T00:00:00Z")
        val rl = createLimiter { now }
        val ip1 = InetAddress.getByName("10.0.0.1")
        val op = Oppy("GET", "/api", ip1, 1L)
        
        // Use up the 3-per-second global limit.
        rl.requirePermits(op)
        rl.requirePermits(op)
        rl.requirePermits(op)
        
        // Fourth request in the same second should be rejected.
        assertFailsWith<RateLimitExceededException> {
            rl.requirePermits(op)
        }
        
        // Advance time by exactly one second.
        now = now.plusSeconds(1)
        // The grants from the previous second are still in the window (they occur at now - window)
        // so the limit is still exceeded.
        assertFailsWith<RateLimitExceededException> {
            rl.requirePermits(op)
        }
        
        // Advance time further so that earlier grants fall out of the window.
        now = now.plusSeconds(1)
        // Now at least one permit should be available.
        rl.requirePermits(op)
    }
    
    @Test
    fun testFoobarsLimit() {
        var now: Instant = Instant.parse("2021-01-01T00:00:00Z")
        val rl = createLimiter { now }
        val ip1 = InetAddress.getByName("10.0.0.1")
        // This operation matches both the "api" and "api/foobars" rules.
        val op = Oppy("GET", "/api/foobars", ip1, 42L)
        
        // For rule "api":
        //   Global grouping: 3 permits/second
        //   IP grouping: 10 permits/second
        // So available = min(3, 10) = 3.
        // For rule "api/foobars":
        //   Global grouping: 1 permit/second
        //   User grouping: 5 permits/second
        // So overall available = min(3, 1) = 1.
        // Thus only one permit should be granted.
        rl.requirePermits(op)
        
        // A second immediate request should fail due to the 1-per-second limit.
        assertFailsWith<RateLimitExceededException> {
            rl.requirePermits(op)
        }
        
        // Advance time to let the window clear.
        now = now.plusSeconds(2)
        rl.requirePermits(op)
    }
    
    @Test
    fun testNoRuleApplies() {
        var now: Instant = Instant.parse("2021-01-01T00:00:00Z")
        val rl = createLimiter { now }
        val ip1 = InetAddress.getByName("10.0.0.1")
        // An operation with a path that does not match any rule.
        val op = Oppy("GET", "/other", ip1, 99L)
        // When no rule applies, the limiter should be effectively unlimited.
        val res = rl.requestPermits(op, 100u, partial = false)
        assertEquals(100u, res.granted)
    }
    
    @Test
    fun testPartialPermit() {
        var now: Instant = Instant.parse("2021-01-01T00:00:00Z")
        val rl = createLimiter { now }
        val ip1 = InetAddress.getByName("10.0.0.1")
        val op = Oppy("GET", "/api", ip1, 1L)
        // Consume 2 of the 3 available permits.
        rl.requirePermits(op)
        rl.requirePermits(op)
        // Now request 2 permits in partial mode.
        // Only 1 should be available.
        val res = rl.requestPermits(op, 2u, partial = true)
        assertEquals(1u, res.granted)
        // The reported available should also be 1.
        assertEquals(1u, res.available)
    }
    
    @Test
    fun testGiveBack() {
        var now: Instant = Instant.parse("2021-01-01T00:00:00Z")
        val rl = createLimiter { now }
        val ip1 = InetAddress.getByName("10.0.0.1")
        val op = Oppy("GET", "/api", ip1, 1L)
        // Request a permit.
        val permits = rl.requestPermits(op, 1u, partial = false)
        assertEquals(1u, permits.granted)
        
        // Capture the current grant history size.
        val historyBefore = rl.grantHistory().size
        
        // Give back the granted permit.
        permits.giveBack(1u)
        
        // The grant should be removed from each matching rule.
        val historyAfter = rl.grantHistory().size
        assertEquals(historyBefore - 1, historyAfter)
        
        // After giving back, a new request should succeed.
        rl.requirePermits(op)
    }
    
    @Test
    fun testMultipleIPsAndUsers() {
        var now: Instant = Instant.parse("2021-01-01T00:00:00Z")
        val rl = createLimiter { now }
        val ip1 = InetAddress.getByName("10.0.0.1")
        val ip2 = InetAddress.getByName("10.0.0.2")
        // Two operations with different IPs (and possibly same user) still share the global limit.
        val op1 = Oppy("GET", "/api", ip1, 1L)
        val op2 = Oppy("GET", "/api", ip2, 1L)
        // Use up the global limit (3 permits/second) across the two IPs.
        rl.requirePermits(op1) // 1st permit
        rl.requirePermits(op2) // 2nd permit
        rl.requirePermits(op1) // 3rd permit, global limit reached
        
        // Any further request—even from a different IP—should fail.
        assertFailsWith<RateLimitExceededException> {
            rl.requirePermits(op2)
        }
    }
}

// Helper extension that calls requestPermits with partial=false and throws if not enough permits were granted.
private fun MultiRateLimiter<MultiRateLimiterTest.Oppy>.requirePermits(op: MultiRateLimiterTest.Oppy, permits: UInt = 1u) {
    val result = requestPermits(op, permits, partial = false)
    if (result.granted < permits) {
        throw RateLimitExceededException("Requested $permits but only granted ${result.granted}")
    }
}
