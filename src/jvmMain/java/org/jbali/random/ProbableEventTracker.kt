package org.jbali.random

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class ProbableEventTracker(
    private val scope: String,
) {
    
    private val log: Logger = LoggerFactory.getLogger(ProbableEventTracker::class.java)
    
    private val stats = ConcurrentHashMap<StatsKey, StatsVal>()
    private data class StatsKey(
        val event: String,
        val chance: Probability,
    ) {
        override fun toString() = "$event@${chance.asUnitNum}"
    }
    private data class StatsVal(
        val opportunities: UInt,
        val triggered: UInt,
    ) {
        val ratio get() = triggered.toDouble() / opportunities.toDouble()
        override fun toString() = "$triggered/$opportunities ($ratio)"
        operator fun plus(b: StatsVal) = StatsVal(opportunities + b.opportunities, triggered + b.triggered)
    }
    
    fun rollAndLog(event: String, chance: Probability, random: Random = Random): Boolean {
        val triggered = chance.roll(random)
        trackAndLog(event, chance, triggered)
        return triggered
    }
    
    fun trackAndLog(event: String, chance: Probability, triggered: Boolean) {
        val evKey = StatsKey(event, chance)
        val evStats = stats.merge(evKey, StatsVal(1u, if (triggered) 1u else 0u), StatsVal::plus)
        log.info("Event $scope/$evKey triggered $evStats")
    }
    
}
