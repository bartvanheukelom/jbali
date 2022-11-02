package org.jbali.hum

import org.jbali.util.cast
import kotlin.test.Test
import kotlin.test.assertEquals

class HumMapTest {
    
    @Test fun testAssociate() {
        val regMap = HumMap.cast<Iterable<Animalia>>().associateWith { it.name }
        val humMap = HumMap.associate(Animalia) { it.name }
        assertEquals(regMap, humMap)
    }
    
}