package org.jbali.random

import org.jbali.math.unitInterval
import kotlin.math.pow
import kotlin.random.Random

inline class Probability(
    val asUnitNum: Double
) {
    companion object {

        fun fromUnitNum(unitNum: Double): Probability {
            require(unitNum in unitInterval) {
                "Illegal probability $unitNum, must be in range [0, 1]"
            }
            return Probability(unitNum)
        }

        fun percentage(percent: Int) = fromUnitNum(percent / 100.0)

        val impossible = Probability(0.0)
        val fiftyFifty = Probability(0.5)
        val certain = Probability(1.0)
    }

    val isImpossible get() = this == impossible
    val isCertain    get() = this == certain

    override fun toString() = asUnitNum.toString()

    fun roll(random: Random = Random): Boolean =
        random.nextDouble() < asUnitNum
}

/**
 * For a sequence of [steps] number of steps, return the chance each step should have
 * of completing successfully to result in a [sequenceSuccessChance] chance of the
 * entire sequence completing successfully.
 */
fun stepSuccessChance(sequenceSuccessChance: Probability, steps: Int): Probability {
    require(steps > 0)

    // sequenceSuccessChance = stepSuccessChance ^ failPoints
    // failPoints√sequenceSuccessChance = sequenceSuccessChance
    // successChance ^ (1/failPoints) = sequenceSuccessChance
    return Probability.fromUnitNum(sequenceSuccessChance.asUnitNum.pow(1.0 / steps))
}
