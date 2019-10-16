package org.jbali.kotser

import kotlinx.serialization.json.JSON
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class BigDecimalSerializerTest {
    @Test
    fun test() {
        assertEquals(""""10"""", JSON.plain.stringify(BigDecimalSerializer, BigDecimal(10)))
        assertEquals(""""12.30"""", JSON.plain.stringify(BigDecimalSerializer, BigDecimal(1230).scaleByPowerOfTen(-2)))

        assertEquals(BigDecimal(1234).scaleByPowerOfTen(-2), JSON.plain.parse(BigDecimalSerializer, """"12.34""""))
        assertEquals(BigDecimal(1230).scaleByPowerOfTen(-2), JSON.plain.parse(BigDecimalSerializer, """"12.30""""))
        assertComparesEqual(BigDecimal(1230).scaleByPowerOfTen(-2), JSON.plain.parse(BigDecimalSerializer, """"12.3""""))
    }
}

fun <T : Comparable<T>> assertComparesEqual(expected: T, actual: T) {
    val comp = expected.compareTo(actual)
    if (comp != 0) throw AssertionError("Expected <$expected>.compareTo(<$actual>) == 0 but was $comp.")
}