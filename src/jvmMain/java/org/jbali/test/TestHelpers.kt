package org.jbali.test

import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Assert that block throws an exception of type T, like assertFailsWith, but also prints that exception (only if it's the expected one)
 */
inline fun <reified T : Throwable> assertFailsWithPrinted(message: String? = null, noinline block: () -> Unit): T =
        assertFailsWith<T>(message, block).also { it.printStackTrace() }

fun assertContains(expectedPart: String, actualFull: String) {
    if (expectedPart !in actualFull) {
        throw AssertionError("Expected part:\n\n${expectedPart}\n\nnot found in actual string:\n\n$actualFull")
    }
}

fun <T> assertListImplementation(list: List<T>) {
    val simple = list.toList()
    assertEquals(list, simple)
    assertEquals(simple, list)

    @Suppress("ReplaceSizeZeroCheckWithIsEmpty")
    assertEquals(list.isEmpty(), list.size == 0)

}

fun assertComparesEqual(expected: BigDecimal, actual: BigDecimal, message: String? = null) {
    // TODO better message that doesn't lose original scale
    val ms = maxOf(expected.scale(), actual.scale())
    assertEquals(expected.setScale(ms), actual.setScale(ms), message)
}
