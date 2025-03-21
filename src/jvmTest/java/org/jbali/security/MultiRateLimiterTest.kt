package org.jbali.security

import org.jbali.util.NanoDuration
import org.jbali.util.NanoTime
import org.jbali.util.logger
import org.junit.Test
import java.net.Inet4Address
import java.net.InetAddress
import java.time.Duration
import java.time.Instant
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MultiRateLimiterTest {
    
    private val log = logger<MultiRateLimiterTest>()
    
    data class Oppy(
        val method: String,
        val path: String,
        val ip: InetAddress,
        val user: Long,
    )
    
    // Helper factory to build a new rate limiter with fixed rules and a mutable clock.
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
                                BurstRate(6u, Duration.ofSeconds(1)),
                                BurstRate(60u, Duration.ofMinutes(1))
                            )
                        ),
                        MultiRateLimiter.Grouping(
                            name = "ip",
                            opGroup = { it.ip },
                            rates = listOf(
                                BurstRate(3u, Duration.ofSeconds(1))
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
                                BurstRate(5u, Duration.ofSeconds(1))
                            )
                        ),
                        MultiRateLimiter.Grouping(
                            name = "user",
                            opGroup = { it.user },
                            rates = listOf(
                                BurstRate(1u, Duration.ofSeconds(1))
                            )
                        )
                    )
                )
            ),
            clock = clock
        )
    }
    
    @Test
    fun testMultiRateLimiterIntegration() {
        var now: Instant = Instant.parse("2021-01-01T00:00:00Z")
        val rl = createLimiter { now }
        val ip1 = InetAddress.getByName("10.0.0.1")
        val ip2 = InetAddress.getByName("10.0.0.2")
        val ip3 = InetAddress.getByName("10.0.0.3")
        val ip4 = InetAddress.getByName("10.0.0.4")
        
        val anne = 13370001L
        val bert = 13370002L
        val carl = 13370003L
        val dave = 13370004L
        
        // Define operations.
        val globalApiOp = Oppy("GET", "/api", ip1, 1L)
        val foobarsOp = Oppy("GET", "/api/foobars", ip1, 42L)
        val otherOp = Oppy("GET", "/other", ip1, 99L)
        
        // === Global API Limit Test ===
        
        // For "/api": the global grouping allows 3 permits per second.
        repeat(3) { rl.requirePermits(globalApiOp) }
        // A 4th permit request in the same second should be rejected.
        assertFailsWith<RateLimitExceededException> {
            rl.requirePermits(globalApiOp)
        }
        
        // Do the same but getting 3 permits in a single request.
        now = now.plusSeconds(2)
        rl.requirePermits(globalApiOp, 3u)
        assertFailsWith<RateLimitExceededException> {
            rl.requirePermits(globalApiOp, 1u)
        }
        
        // Requests should be allowed again after the rate limit window has passed exactly.
        now = now.plusSeconds(1)
        rl.requirePermits(globalApiOp, 1u)
        
        // === Foobars Limit Test ===
        
        // Jump forward to clear previous history.
        now = now.plusSeconds(10)
        
        // For "/api/foobars":
        //   "api" rule: Global (6/sec) and IP (10/sec) => effective 3/sec.
        //   "api/foobars" rule: Global (1/sec) and User (5/sec) => effective 1/sec.
        // Overall, only one permit should be granted.
        rl.requirePermits(foobarsOp, 1u)
        // A second immediate request should fail.
        assertFailsWith<RateLimitExceededException> {
            rl.requirePermits(foobarsOp, 1u)
        }
        now = now.plusSeconds(2)
        rl.requirePermits(foobarsOp, 1u)
        
        // === No Rule Applies Test ===
        // For a request that doesn't match any rule, the limiter should be effectively unlimited.
        val res = rl.requestPermits(otherOp, 100u, partial = false)
        assertEquals(100u, res.granted)
        
        // === Partial Permit Test ===
        now = now.plusSeconds(10)
        // Consume 2 of the 3 available permits for "/api".
        rl.requirePermits(globalApiOp, 2u)
        // Now request 2 permits in partial mode—only 1 should be available.
        val partialRes = rl.requestPermits(globalApiOp, 2u, partial = true)
        assertEquals(1u, partialRes.granted)
        assertEquals(1u, partialRes.available)
        
        // === Give Back Test ===
        now = now.plusSeconds(10)
        val permits = rl.requestPermits(globalApiOp, 3u, partial = false)
        assertEquals(3u, permits.granted)
        
        // Before giving back, a new request should fail.
        assertFailsWith<RateLimitExceededException> {
            rl.requirePermits(globalApiOp, 1u)
        }
        
        val historyBefore = rl.grantHistory()
        permits.giveBack(3u)
        val historyAfter = rl.grantHistory()
        assertEquals(historyBefore.size - 1, historyAfter.size)
        assertEquals(historyBefore.sumOf { it.granted } - 3u, historyAfter.sumOf { it.granted })
        
        // After giving back, a new request should succeed.
        rl.requirePermits(globalApiOp, 1u)

        // === Multiple IPs and Users Test ===
        now = now.plusSeconds(10)

        // First, one IP hits its per-IP limit (3 permits/second).
        rl.requirePermits(Oppy("GET", "/api", ip1, anne)) // ip1: 1, global: 1
        rl.requirePermits(Oppy("GET", "/api", ip1, anne)) // ip1: 2, global: 2
        rl.requirePermits(Oppy("GET", "/api", ip1, anne)) // ip1: 3, global: 3
        assertFailsWith<RateLimitExceededException> {
            rl.requirePermits(Oppy("GET", "/api", ip1, anne)) // 4th request from ip1 should fail.
        }

        // Now, use different IPs to drive the global count to 6.
        rl.requirePermits(Oppy("GET", "/api", ip2, bert))  // global: 4, ip2: 1
        rl.requirePermits(Oppy("GET", "/api", ip3, carl))  // global: 5, ip3: 1
        rl.requirePermits(Oppy("GET", "/api", ip4, dave))  // global: 6, ip4: 1

        // With a global limit of 6, any additional request should be rejected,
        // even if it's from an IP that hasn't hit its own limit.
        assertFailsWith<RateLimitExceededException> {
            rl.requirePermits(Oppy("GET", "/api", ip2, bert))
        }

    }
    
    @Test fun testPerformance() {
        
        val rules = 100
        
        val numIps = 100
        val numUsers = 200
        
        val repeatTest = 10
        val testPreseed = 10_000
        val testDuration = NanoDuration.ofSeconds(1.0)
        val testRequestsPerIter = 100
        
        var now: Instant = Instant.parse("2021-01-01T00:00:00Z")
        
        log.info("Initializing rate limiter with $rules rules.")
        val rl: MultiRateLimiter<Oppy> = MultiRateLimiter(
            rules = (0 until rules).map { i ->
                val rePath = Regex(if (i % 2 == 0) {
                    "^/api/fun$i(/.*)?$"
                } else {
                    "^/[^/]+/fun$i(/.*)?$"
                })
                MultiRateLimiter.Rule(
                    name = "rule-$i",
                    scope = {
                        it.method == "GET" && rePath.matches(it.path)
                    },
                    groupings = listOf(
                        MultiRateLimiter.Grouping(
                            name = "global",
                            opGroup = { Unit },
                            rates = listOf(BurstRate(999999u, Duration.ofSeconds(1)))
                        ),
                        MultiRateLimiter.Grouping(
                            name = "ip",
                            opGroup = { it.ip },
                            rates = listOf(BurstRate(999999u, Duration.ofSeconds(1)))
                        ),
                        MultiRateLimiter.Grouping(
                            name = "user",
                            opGroup = { it.user },
                            rates = listOf(BurstRate(999999u, Duration.ofSeconds(1)))
                        )
                    )
                )
            },
            clock = { now }
        )
        
        var rulesEvaled = 0
        rl.onRuleEvaluated.listen { re ->
            rulesEvaled += minOf(re.toString().length + 1, 1) // make sure toString() is not optimized away
        }
        
        val rng = Random(383495493912)
        
        val ips = (0 until numIps).map {
            Inet4Address.getByAddress(byteArrayOf(10, 0, 0, (10 + it).toByte()))
        }
        val users = (0 until numUsers).map { it.toLong() }
        
        fun randomRequest() {
            rl.requirePermits(Oppy(
                "GET", "/api/fun${rng.nextInt(rules)}",
                ip = ips.random(rng),
                user = users.random(rng)
            ))
        }
        
        repeat(repeatTest) { iteration ->
            log.info("Iteration ${iteration + 1}: Advancing time by 24 hours to clear history.")
            now = now.plus(Duration.ofHours(24))
            
            val realStart = NanoTime.now()
            
            log.info("Iteration ${iteration + 1}: Preseeding with $testPreseed random requests.")
            repeat(testPreseed) {
                try {
                    randomRequest()
                } catch (e: RateLimitExceededException) {
                    // Ignore exceeded permits during preseeding.
                }
            }
            val tsAfterSeed = NanoTime.now()
            log.info("Iteration ${iteration + 1}: Preseeded in ${NanoDuration.between(realStart, tsAfterSeed)}")
            
            val targetEnd = tsAfterSeed + testDuration
            var count = 0L
            rulesEvaled = 0
            
            // Loop until the simulated clock has advanced by testDuration.
            while (NanoTime.now() < targetEnd) {
                repeat(testRequestsPerIter) {
                    try {
                        randomRequest()
                    } catch (e: RateLimitExceededException) {
                        // Ignore exceptions to keep counting throughput.
                    }
                }
                count += testRequestsPerIter
            }
            val realElapsed = NanoDuration.since(realStart)
            log.info("Iteration ${iteration + 1}: Completed $count requests, evaluated $rulesEvaled rules, in $realElapsed")
        }
        
        log.info("Performance test completed.")
    }
    
    // results:
    // 36000 - initial
    // 40000 - regex match instead of simple string equals (expected it to be worse)
    // 31000 - toString on rule eval
}
