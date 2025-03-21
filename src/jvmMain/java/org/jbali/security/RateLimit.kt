package org.jbali.security

import java.time.Duration
import java.time.Instant

data class BurstRate(
    val permits: UInt,
    val window: Duration,
) {
    
    operator fun times(multiplier: Int) = BurstRate(permits * multiplier.toUInt(), window)
    
}

data class FreezeFrame(
    val now: Instant,
)
