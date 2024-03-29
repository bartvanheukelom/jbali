package org.jbali.random

import org.jbali.math.unitInterval
import org.jbali.util.boxed
import org.jetbrains.annotations.Range
import kotlin.math.pow
import kotlin.random.Random

@JvmInline
value class Probability private constructor(
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
        fun oneIn(count: UInt) = Probability(1.0 / count.toDouble())
        
        @JvmStatic fun oneIn(count: Int) = fromUnitNum(1.0 / count.toDouble())
            // force wrapped (non-inline) return type (in an extra box, oh well)
            .boxed()
        
        @JvmStatic fun wrap(asUnitNum: Double) = Probability(asUnitNum).boxed()
    
        val impossible = Probability(0.0)
        val fiftyFifty = Probability(0.5)
        val certain = Probability(1.0)
    }

    val isImpossible get() = this == impossible
    val isCertain    get() = this == certain

    override fun toString() = asUnitNum.toString()

    @JvmOverloads
    fun roll(random: Random = Random): Boolean =
        random.nextDouble() < asUnitNum
    
    fun inverse() = Probability(1.0 - asUnitNum)
}

/**
 * For a sequence of [steps] number of steps, return the chance each step should have
 * of completing successfully to result in a [sequenceSuccessChance] chance of the
 * entire sequence completing successfully.
 */
fun stepSuccessChance(sequenceSuccessChance: Probability, steps: @Range(from = 0, to = Int.MAX_VALUE.toLong()) Int): Probability {
    require(steps > 0)

    // sequenceSuccessChance = stepSuccessChance ^ failPoints
    // failPoints√sequenceSuccessChance = sequenceSuccessChance
    // successChance ^ (1/failPoints) = sequenceSuccessChance
    return Probability.fromUnitNum(sequenceSuccessChance.asUnitNum.pow(1.0 / steps))
}
