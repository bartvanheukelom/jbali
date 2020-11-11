package org.jbali.threeten

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ThreeTenUtilsKtTest {

    @Test
    fun instantBounds() {

        println("longmax = ${Long.MAX_VALUE}")
        println("instantMaxDate = $instantMaxDate ( ${instantMaxDate.epochSecond}, ${instantMaxDate.nano} -> ${instantMaxDate.toEpochMilli()} )")

        assertEquals(Long.MAX_VALUE, instantMaxDate.toEpochMilli())
        assertEquals(instantMaxDate, instantMaxDate.toDate().toInstant())

        // lose precision but don't throw
        assertEquals(Long.MAX_VALUE, instantVeryMaxDate.toEpochMilli())
        assertEquals(instantMaxDate, instantVeryMaxDate.toDate().toInstant())

        val postMax = instantMaxDate.plusNanos(1_000_000L)
        assertEquals(postMax.minusNanos(1), instantVeryMaxDate)
        assertEquals(postMax, instantVeryMaxDate.plusNanos(1L))

        assertFailsWith<ArithmeticException> { postMax.toEpochMilli() }
        assertFailsWith<ArithmeticException> { postMax.toDate() }

    }

}