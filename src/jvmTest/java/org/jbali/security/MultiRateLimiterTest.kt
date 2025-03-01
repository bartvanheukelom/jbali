package org.jbali.security

import org.junit.Test
import java.net.InetAddress
import java.time.Duration
import java.time.Instant
import kotlin.test.assertFailsWith

class MultiRateLimiterTest {
    
    data class Oppy(
        val method: String,
        val path: String,
        val ip: InetAddress,
        val user: Long,
    )
    
    @Test fun test() {
        var now: Instant = Instant.parse("2021-01-01T00:00:00Z")
        val rl = MultiRateLimiter<Oppy>(
            rules = listOf(
                MultiRateLimiter.Rule(
                    name = "api",
                    scope = { it.path == "/api" || it.path.startsWith("/api/") },
                    groupings = listOf(
                        MultiRateLimiter.Grouping(
                            name = "global",
                            opGroup = {},
                            rates = listOf(
                                BurstRate( 3u, Duration.ofSeconds(1)),
                                BurstRate(60u, Duration.ofMinutes(1)),
                            ),
                        ),
                        MultiRateLimiter.Grouping(
                            name = "ip",
                            opGroup = { it.ip },
                            rates = listOf(
                                BurstRate(10u, Duration.ofSeconds(1)),
                            ),
                        ),
                    ),
                ),
                MultiRateLimiter.Rule(
                    name = "api/foobars",
                    scope = { it.path == "/api/foobars" },
                    groupings = listOf(
                        MultiRateLimiter.Grouping(
                            name = "global",
                            opGroup = {},
                            rates = listOf(
                                BurstRate( 1u, Duration.ofSeconds(1)),
                            ),
                        ),
                        MultiRateLimiter.Grouping(
                            name = "user",
                            opGroup = { it.user },
                            rates = listOf(
                                BurstRate( 5u, Duration.ofSeconds(1)),
                            ),
                        ),
                    ),
                ),
            ),
            clock = { now },
        )
        
        val ip3 = InetAddress.getByName("10.1.2.3")
        
        val user1337 = 1337L
        
        repeat(3) {
            rl.requirePermits(Oppy("GET", "/api", ip3, user1337))
        }
        assertFailsWith<RateLimitExceededException> {
            rl.requirePermits(Oppy("GET", "/api", ip3, user1337))
        }
        
        // clear the window, but only just
        now = now.plusSeconds(1)
        assertFailsWith<RateLimitExceededException> { // TODO make it not fail
            rl.requirePermits(Oppy("GET", "/api", ip3, user1337))
        }
        
        // now... clear-ly (padum-tss)
        now = now.plusSeconds(1)
        rl.requirePermits(Oppy("GET", "/api", ip3, user1337))
        
    }
    
}
