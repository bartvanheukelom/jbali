package org.jbali.hum

import org.jbali.collect.ListSet
import org.jbali.hum.Animalia.Carnivora
import org.jbali.hum.Animalia.Rodentia
import org.jbali.kotser.assertSerialization
import org.jbali.kotser.jsonSerializer
import org.jbali.serialize.JavaSerializer
import org.jbali.serialize.JavaSerializer.read
import org.jbali.serialize.JavaSerializer.write
import org.jbali.util.forEachWrappingExceptions
import org.jbali.util.toHexString
import kotlin.test.*

class HumTest {

    private val values: ListSet<Animalia> get() = Animalia

    @ExperimentalStdlibApi
    @Test fun testBasics() {

        assertFalse(values.isEmpty())

        values.withIndex().forEachWrappingExceptions { (i, v) ->
            System.err.println("[$i] $v ordinal=${v.ordinal}")

            assertEquals(i, v.ordinal)

            // serialization makes it hard
            JavaSerializer.assertWriteReplace(v.javaClass)

            val ser = write(v)
            System.err.println("ser.dec = ${ser.decodeToString().map {
                if (it.isISOControl()) {
                    '_'
                } else {
                    it
                }
            }.toCharArray().concatToString()}")
            System.err.println("ser.hex = ${ser.toHexString()}")

            val copy = read(ser) as Animalia
            System.err.println("$v#${System.identityHashCode(v)} ====> $copy#${System.identityHashCode(copy)}")
            assertSame(v, copy)
        }

        assertEquals(values.filterIsInstance<Carnivora>(), Carnivora.values)
        assertEquals(values.filterIsInstance<Carnivora.Felidae>(), Carnivora.Felidae.values)

        assertTrue(Carnivora.Felidae.FCatus  in Carnivora.Felidae)
        assertTrue(Carnivora.Felidae.FCatus  in Carnivora)
        assertTrue(Carnivora.Felidae.FCatus !in Rodentia)
        assertTrue(Carnivora.Felidae.FCatus  in Animalia)

        assertTrue(Carnivora.Felidae in Carnivora)
        assertTrue(Carnivora.NandiniaBinotata  in Carnivora)
        assertTrue(Rodentia.MusMusculus !in Carnivora)
        assertTrue(Rodentia !in Carnivora)

    }

    @Test fun testNames() {
        assertEquals("Carnivora.Felidae.FCatus", Carnivora.Felidae.FCatus.name)
        assertEquals("Carnivora.Felidae.PLeo", Carnivora.Felidae.PLeo.name)
        assertEquals("Carnivora.Felidae", Carnivora.Felidae.name)
        assertEquals("Carnivora.NandiniaBinotata", Carnivora.NandiniaBinotata.name)
        assertEquals("Carnivora", Carnivora.name)
        assertEquals("Rodentia", Rodentia.name)
        assertEquals("Rodentia.MusMusculus", Rodentia.MusMusculus.name)
        assertEquals("", Animalia.name)

        values.withIndex().forEachWrappingExceptions { (_, v) ->
            assertEquals(v.name, v.toString())
        }
    }
    
    @Test fun testNodesByLocalName() {
        
        // BASE: root
        assertEquals(Animalia, Animalia.descendantByLocalName("")) // self
        assertEquals(Carnivora, Animalia.descendantByLocalName("Carnivora")) // direct child
        assertEquals(Carnivora.Felidae, Animalia.descendantByLocalName("Carnivora.Felidae")) // descendant group
        assertEquals(Carnivora.Felidae.FCatus, Animalia.descendantByLocalName("Carnivora.Felidae.FCatus")) // descendant value
        
        assertFailsWith<NoSuchElementException> { Animalia.descendantByLocalName("Robotica") }
        assertFailsWith<NoSuchElementException> { Animalia.descendantByLocalName("Robotica.RMega") }
        
        assertFailsWith<IllegalArgumentException> { Animalia.descendantByLocalName("Carnivora.Felidae.FCatus.FCatusSupercutus") }
        
        // BASE: group
        assertEquals(Carnivora, Carnivora.descendantByLocalName("")) // self
        assertEquals(Carnivora.Felidae, Carnivora.descendantByLocalName("Felidae")) // direct child
        assertEquals<Any>(Carnivora.Felidae.FCatus, Carnivora.descendantByLocalName("Felidae.FCatus")) // descendant value
        // BASE: value - TODO
//        assertEquals(Carnivora.Felidae.FCatus, Carnivora.Felidae.FCatus.descendantByName("")) // self
    }
    
    @Test fun testNodeLocalNames() {
        // BASE: root
        assertEquals("", Animalia.localNameOf(Animalia)) // self
        assertEquals("Carnivora", Animalia.localNameOf(Carnivora)) // direct child
        assertEquals("Carnivora.Felidae", Animalia.localNameOf(Carnivora.Felidae)) // descendant group
        assertEquals("Carnivora.Felidae.FCatus", Animalia.localNameOf(Carnivora.Felidae.FCatus)) // descendant value
        
        // BASE: group
        assertEquals("", Carnivora.localNameOf(Carnivora)) // self
        assertEquals("Felidae", Carnivora.localNameOf(Carnivora.Felidae)) // direct child
        assertEquals("Felidae.FCatus", Carnivora.localNameOf(Carnivora.Felidae.FCatus)) // descendant value
        
        assertFailsWith<IllegalArgumentException> { Carnivora.localNameOf(Rodentia.MusMusculus) }
            .also { assertEquals("Rodentia.MusMusculus is not a descendant of Carnivora", it.message) }
        
        // BASE: value - TODO
//        assertEquals("", Carnivora.Felidae.FCatus.localNameOf(Carnivora.Felidae.FCatus)) // self
    }
    
    @Test fun testNodeSerializer() {
        // BASE: root
        val serRoot = jsonSerializer(Animalia.nodeSerializer)
        serRoot.assertSerialization(Animalia, """""""")
        serRoot.assertSerialization(Carnivora, """"Carnivora"""")
        serRoot.assertSerialization(Carnivora.Felidae, """"Carnivora.Felidae"""")
        serRoot.assertSerialization(Carnivora.Felidae.FCatus, """"Carnivora.Felidae.FCatus"""")
        // BASE: group
        val serGroup = jsonSerializer(Carnivora.nodeSerializer)
        serGroup.assertSerialization(Carnivora, """""""")
        serGroup.assertSerialization(Carnivora.Felidae, """"Felidae"""")
//        serGroup.assertSerialization(Carnivora.Felidae.FCatus, """"Felidae.FCatus"""") - TODO why not ok?
    }
    
}
