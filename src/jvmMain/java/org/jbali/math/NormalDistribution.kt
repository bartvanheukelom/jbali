package org.jbali.math

import kotlinx.serialization.Serializable

@Serializable
data class NormalDistribution(
    val mean: Double,
    val sd: Double
) {
    companion object {
        val standard = NormalDistribution(mean = 0.0, sd = 1.0)
    }
}