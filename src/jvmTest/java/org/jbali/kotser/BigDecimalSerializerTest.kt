package org.jbali.kotser

import org.jbali.kotser.std.BigDecimalSerializer
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class BigDecimalSerializerTest {

    @Test
    fun test() {
        assertEquals(""""10"""", DefaultJson.plain.stringify(BigDecimalSerializer, BigDecimal(10)))
        assertEquals(""""12.30"""", DefaultJson.plain.stringify(BigDecimalSerializer, BigDecimal(1230).scaleByPowerOfTen(-2)))

        assertEquals(BigDecimal(1234).scaleByPowerOfTen(-2), DefaultJson.plain.parse(BigDecimalSerializer, """"12.34""""))
        assertEquals(BigDecimal(1230).scaleByPowerOfTen(-2), DefaultJson.plain.parse(BigDecimalSerializer, """"12.30""""))
        assertComparesEqual(BigDecimal(1230).scaleByPowerOfTen(-2), DefaultJson.plain.parse(BigDecimalSerializer, """"12.3""""))
    }
}

fun <T : Comparable<T>> assertComparesEqual(expected: T, actual: T) {
    val comp = expected.compareTo(actual)
    if (comp != 0) throw AssertionError("Expected <$expected>.compareTo(<$actual>) == 0 but was $comp.")
}
