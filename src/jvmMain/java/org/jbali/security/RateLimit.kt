package org.jbali.security

import java.time.Duration

data class BurstRate(
    val permits: UInt,
    val window: Duration,
)
