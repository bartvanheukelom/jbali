package org.jbali.security

import org.jbali.events.MutableObservable
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DynamicMultiRateLimiterTest {
    
    @Test
    fun testDynamicUpdateWithLocalRulesFunction() {
        // Fixed clock so that all permits fall within the same time window.
        val fixedTime = Instant.parse("2021-01-01T00:00:00Z")
        val clock = { fixedTime }
        
        // Local helper: returns a ruleset with the given permits per second.
        fun rulesFor(permitsPerSecond: UInt) = listOf(
            MultiRateLimiter.Rule<Unit>(
                name = "global",
                scope = { true },
                groupings = listOf(
                    MultiRateLimiter.Grouping(
                        name = "global",
                        opGroup = { Unit },
                        rates = listOf(BurstRate(permitsPerSecond, Duration.ofSeconds(1)))
                    )
                )
            )
        )
        
        // Create the mutable observable with an initial limit of 3 permits per second.
        val ruleObservable = MutableObservable(rulesFor(3u), "RulesObservable")
        val dynamicLimiter = DynamicMultiRateLimiter<Unit>(ruleObservable, clock)
        
        // With the initial rules, three permits are allowed in the same window.
        dynamicLimiter.requirePermits(Unit)
        dynamicLimiter.requirePermits(Unit)
        dynamicLimiter.requirePermits(Unit)
        assertFailsWith<RateLimitExceededException> {
            dynamicLimiter.requirePermits(Unit) // 4th request should be rejected.
        }
        
        // Update the rules to allow 5 permits per second.
        ruleObservable.value = rulesFor(5u)
        
        // The previous three grants are preserved; two more permits are allowed.
        dynamicLimiter.requirePermits(Unit) // 4th permit (allowed)
        dynamicLimiter.requirePermits(Unit) // 5th permit (allowed)
        // But a 6th one should be rejected.
        assertFailsWith<RateLimitExceededException> {
            dynamicLimiter.requirePermits(Unit)
        }
    }
}
