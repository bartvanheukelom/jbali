package org.jbali.math

import kotlin.test.Test
import kotlin.test.assertTrue

class RangeSetParserTest {
    
    @Test
    fun testInt() {
        val range = RangeSetParser.int.parse("..5,10..12,70,100..")
        assertTrue(-100  in range)
        assertTrue(   5  in range)
        assertTrue(   6 !in range)
        assertTrue(   9 !in range)
        assertTrue(  10  in range)
        assertTrue(  11  in range)
        assertTrue(  66 !in range)
        assertTrue( 100  in range)
        assertTrue(9999  in range)
    }
    
}