package org.jbali.threeten

import org.jbali.test.assertListImplementation
import java.time.DayOfWeek
import kotlin.test.*
import org.threeten.extra.YearQuarter.of as yq
import org.threeten.extra.YearWeek.of as yw
import java.time.LocalDate.of as ld
import java.time.Year.of as y
import java.time.YearMonth.of as ym

class LocalDateUnitRangeTest {

    @Test fun testLocalDate() {

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

    @Test fun testYearWeek() {

        assertFailsWith<IllegalArgumentException> {
            yw(2020, 20) until yw(2020, 19)
        }

        val r = yw(2020, 10) until yw(2020, 20)

        assertEquals(10, r.size)
        assertFalse(r.isEmpty())
        assertEquals(yw(2020, 10), r.first())
        assertEquals(yw(2020, 19), r.last())
        assertListImplementation(r)

        assertTrue(yw(2020, 9) !in r)
        assertTrue(yw(2020, 10) in r)
        assertTrue(yw(2020, 15) in r)
        assertTrue(yw(2020, 19) in r)
        assertTrue(yw(2020, 20) !in r)
        assertTrue(yw(2021, 5) !in r)

        assertTrue(r.first().atDay(DayOfWeek.MONDAY).minusDays(1) !in r)
        assertTrue(r.first().atDay(DayOfWeek.MONDAY)               in r)
        assertTrue(r.last() .atDay(DayOfWeek.SUNDAY)               in r)
        assertTrue(r.last() .atDay(DayOfWeek.SUNDAY).plusDays(1)  !in r)

        assertEquals((10 until 20).toList(), r.map { it.week })

        val cm = yw(2019, 10) until yw(2020, 10)
        assertEquals(52, cm.size)
        val cmb = yw(2020, 10) until yw(2021, 10)
        assertEquals(53, cmb.size)

        assertListImplementation(cm)

        val empty = yw(2020, 10) until yw(2020, 10)
        assertEquals(0, empty.size)
        assertTrue(empty.isEmpty())

        assertListImplementation(empty)

    }

    @Test fun testYearMonth() {

        assertFailsWith<IllegalArgumentException> {
            ym(2020, 10) until ym(2020, 9)
        }

        val r = ym(2020, 10) until ym(2020, 12)

        assertEquals(2, r.size)
        assertFalse(r.isEmpty())
        assertEquals(ym(2020, 10), r.first())
        assertEquals(ym(2020, 11), r.last())
        assertListImplementation(r)

        assertTrue(ym(2020, 9) !in r)
        assertTrue(ym(2020, 10) in r)
        assertTrue(ym(2020, 11) in r)
        assertTrue(ym(2020, 12) !in r)
        assertTrue(ym(2021, 5) !in r)

        assertTrue(r.first().atDay(1).minusDays(1) !in r)
        assertTrue(r.first().atDay(1)               in r)
        assertTrue(r.last() .atDay(30)               in r)
        assertTrue(r.last() .atDay(30).plusDays(1)  !in r)

        assertEquals((10 until 12).toList(), r.map { it.monthValue })

        val cm = ym(2019, 10) until ym(2020, 10)
        assertEquals(12, cm.size)

        assertListImplementation(cm)

        val empty = ym(2020, 10) until ym(2020, 10)
        assertEquals(0, empty.size)
        assertTrue(empty.isEmpty())

        assertListImplementation(empty)

    }

    @Test fun testYearQuarter() {

        assertFailsWith<IllegalArgumentException> {
            yq(2020, 2) until yq(2020, 1)
        }

        val r = yq(2020, 1) until yq(2020, 3)

        assertEquals(2, r.size)
        assertFalse(r.isEmpty())
        assertEquals(yq(2020, 1), r.first())
        assertEquals(yq(2020, 2), r.last())
        assertListImplementation(r)

        assertTrue(yq(2019, 4) !in r)
        assertTrue(yq(2020, 1)  in r)
        assertTrue(yq(2020, 2)  in r)
        assertTrue(yq(2020, 3) !in r)
        assertTrue(yq(2020, 4) !in r)
        assertTrue(yq(2021, 1) !in r)

        assertTrue(r.first().atDay(1).minusDays(1)         !in r)
        assertTrue(r.first().atDay(1)                       in r)
        assertTrue(r.last() .atEndOfQuarter()               in r)
        assertTrue(r.last() .atEndOfQuarter().plusDays(1)  !in r)

        assertEquals((1 until 3).toList(), r.map { it.quarterValue })

        val cm = yq(2019, 1) until yq(2020, 1)
        assertEquals(4, cm.size)

        assertListImplementation(cm)

        val empty = yq(2020, 1) until yq(2020, 1)
        assertEquals(0, empty.size)
        assertTrue(empty.isEmpty())

        assertListImplementation(empty)

    }

    @Test fun testYear() {

        assertFailsWith<IllegalArgumentException> {
            y(2020) until y(2019)
        }

        val r = y(2020) until y(2030)

        assertEquals(10, r.size)
        assertFalse(r.isEmpty())
        assertEquals(y(2020), r.first())
        assertEquals(y(2029), r.last())
        assertListImplementation(r)

        assertTrue(y(2019) !in r)
        assertTrue(y(2020)  in r)
        assertTrue(y(2025)  in r)
        assertTrue(y(2029)  in r)
        assertTrue(y(2030) !in r)
        assertTrue(y(2031) !in r)

        assertTrue(r.first().atDay(1).minusDays(1) !in r)
        assertTrue(r.first().atDay(1)               in r)
        assertTrue(r.last() .plusYears(1).atDay(1).minusDays(1)               in r)
        assertTrue(r.last() .plusYears(1).atDay(1)  !in r)

        assertEquals((2020 until 2030).toList(), r.map { it.value })

        val cm = y(2019) until y(2020)
        assertEquals(1, cm.size)

        assertListImplementation(cm)

        val empty = y(2020) until y(2020)
        assertEquals(0, empty.size)
        assertTrue(empty.isEmpty())

        assertListImplementation(empty)

    }

}
