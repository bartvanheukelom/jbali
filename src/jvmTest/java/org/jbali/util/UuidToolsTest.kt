package org.jbali.util

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class UuidToolsTest {
    
    companion object {
        private val bb = UUID.fromString("bb4633d3-3061-4bb7-bc1c-35d44d8d0c81")
        private val bbHex = bb.toString().replace("-", "")
    }
    
    @Test
    fun testUuid5() {
        // expected result acquired from https://www.uuidtools.com/v5
        assertEquals(UUID.fromString("af56665b-18f9-5238-a801-a48ee31f9589"),
            uuid5(bb, "foobar".toByteArray()))
    }
    
    @Test fun testToBytes() {
        assertArrayEquals(HexBytes.parseHex(bbHex), bb.toBytes())
    }
    
    @Test fun testFromBytes() {
        assertEquals(bb, uuidFromBytes(HexBytes.parseHex(bbHex)))
    }
    
}