package org.jbali.random

import org.jbali.util.Box
import org.jbali.util.boxed
import org.jetbrains.annotations.Range
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProbabilityKtTest {

    @Test
    fun testStepSuccessChance() {

        fun ssc(t: Double, s: @Range(from = 0, to = Long.MAX_VALUE) Int): Double =
            stepSuccessChance(Probability.fromUnitNum(t), s).asUnitNum

        assertEquals(1.0000000000000000, ssc(1.00, 1))
        assertEquals(1.0000000000000000, ssc(1.00, 5))

        assertEquals(0.5000000000000000, ssc(0.50, 1))
        assertEquals(0.5000000000000000, ssc(0.25, 2))

        assertEquals(0.9791483623609768, ssc(0.90, 5))
        assertEquals(0.9440875112949020, ssc(0.75, 5))
        assertEquals(0.8312895542475669, ssc(0.33, 6))

        assertEquals(0.0000000000000000, ssc(0.00, 5))

        // bad chance
        assertFailsWith<IllegalArgumentException> { ssc(-0.1,  7) }
        assertFailsWith<IllegalArgumentException> { ssc( 1.1,  7) }

        // bad steps
        assertFailsWith<IllegalArgumentException> { ssc( 0.1,  0) }
        assertFailsWith<IllegalArgumentException> { ssc( 0.1, -4) }
    }
    
    @Test
    fun testStepSuccessChanceBoxed() {
    
        val totalSuccessChance: Box<Probability> = Probability.wrap(0.9)
        assertEquals(0.9, totalSuccessChance.contents.asUnitNum)
        
        val stepSuccessChance: Box<Probability> = stepSuccessChanceHelper(totalSuccessChance, 8)
        assertEquals(0.9869162813660015, stepSuccessChance.contents.asUnitNum, absoluteTolerance = 0.0001)
        
    }

}

private fun stepSuccessChanceHelper(sequenceSuccessChance: Box<Probability>, steps: Int) =
    stepSuccessChance(sequenceSuccessChance.unboxed(), steps).boxed()