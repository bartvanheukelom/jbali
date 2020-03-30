package org.jbali.math

import org.junit.Test

import org.junit.Assert.*
import kotlin.math.pow
import kotlin.test.assertFailsWith

class MathKtTest {

    @Test
    fun testPowerOf10() {

        for (n in 0..18) {
            kotlin.test.assertEquals(powerOf10(n), (10.0).pow(n).toLong())
        }

        assertFailsWith<IndexOutOfBoundsException> {
            powerOf10(-1)
        }
        assertFailsWith<IndexOutOfBoundsException> {
            powerOf10(19)
        }

    }
}