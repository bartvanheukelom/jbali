package org.jbali.security

import java.time.Duration
import java.time.Instant

data class BurstRate(
    val permits: UInt,
    val window: Duration,
)

data class FreezeFrame(
    val now: Instant,
)
