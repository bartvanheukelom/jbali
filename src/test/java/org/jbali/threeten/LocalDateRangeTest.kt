package org.jbali.threeten

import org.jbali.test.assertListImplementation
import java.time.LocalDate
import kotlin.test.*

class LocalDateRangeTest {

    @Test fun test() {

        fun ld(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d)

        assertFailsWith<IllegalArgumentException> {
            ld(2020, 8, 20) until ld(2020, 8, 19)
        }

        val r = ld(2020, 8, 10) until ld(2020, 8, 20)

        assertEquals(10, r.size)
        assertFalse(r.isEmpty())
        assertEquals(ld(2020, 8, 10), r.first())
        assertEquals(ld(2020, 8, 19), r.last())
        assertListImplementation(r)

        assertTrue(ld(2020, 8, 9) !in r)
        assertTrue(ld(2020, 8, 10) in r)
        assertTrue(ld(2020, 8, 15) in r)
        assertTrue(ld(2020, 8, 19) in r)
        assertTrue(ld(2020, 8, 20) !in r)
        assertTrue(ld(2021, 8, 5) !in r)

        assertEquals((10 until 20).toList(), r.map { it.dayOfMonth })

        val cm = ld(2020, 8, 10) until ld(2020, 9, 10)
        assertEquals(31, cm.size)

        assertListImplementation(cm)

        val empty = ld(2020, 5, 10) until ld(2020, 5, 10)
        assertEquals(0, empty.size)
        assertTrue(empty.isEmpty())

        assertListImplementation(empty)

    }

}
