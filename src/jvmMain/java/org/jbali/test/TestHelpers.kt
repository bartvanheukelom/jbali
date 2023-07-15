package org.jbali.test

import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Assert that block throws an exception of type T, like assertFailsWith, but also prints that exception (only if it's the expected one)
 */
inline fun <reified T : Throwable> assertFailsWithPrinted(message: String? = null, noinline block: () -> Unit): T =
        assertFailsWith<T>(message, block).also { it.printStackTrace() }

class FailingContext {
    var thrown: Throwable? = null
    fun throwExpected(t: Throwable = RuntimeException("Expected exception")) {
        if (thrown == null) {
            thrown = t
        } else {
            throw AssertionError("Already threw $thrown")
        }
        throw t
    }
}
inline fun assertFailsWithThrowable(block: FailingContext.() -> Unit) {
    val ctx = FailingContext()
    try {
        ctx.block()
    } catch (e: Throwable) {
        if (e !== ctx.thrown) {
            throw e
        }
    }
    if (ctx.thrown == null) {
        throw AssertionError("Expected something to be thrown, but nothing was")
    } else {
        throw AssertionError("Expected ${ctx.thrown} to be thrown, but nothing was caught")
    }
}


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

//fun <T : Comparable<T>> assertComparesEqual(expected: T, actual: T, message: String? = null) {
//    // TODO better message that doesn't lose original scale
//    val ms = maxOf(expected.scale(), actual.scale())
//    assertEquals(expected.setScale(ms), actual.setScale(ms), message)
//}

fun assertComparesEqual(expected: BigDecimal, actual: BigDecimal, message: String? = null) {
    // TODO better message that doesn't lose original scale
    val ms = maxOf(expected.scale(), actual.scale())
    assertEquals(expected.setScale(ms), actual.setScale(ms), message)
}

// this name makes it pop up as suggestion when asserting a compatible type
fun <K, V> assertEquals(expected: Map<K, V>, actual: Map<K, V>) =
    assertMapEquals(expected, actual)
// this name is unambiguous
fun <K, V> assertMapEquals(expected: Map<K, V>, actual: Map<K, V>) {
    val missingKeys = expected.keys - actual.keys
    assert(missingKeys.isEmpty()) {
        "Actual Map is missing expected keys $missingKeys"
    }
    val extraKeys = actual.keys - expected.keys
    assert(extraKeys.isEmpty()) {
        "Actual Map has unexpected keys $extraKeys"
    }
    expected.forEach { (k, v) ->
        assertEquals(v, actual[k], "Map value for key $k")
    }
    
    // in case the above is buggy, or either map's implementation is buggy
    kotlin.test.assertEquals(expected, actual, "Map fallback")
}


// for these extension functions, use "must" instead of "assert" because
// it's otherwise very easy to accidentally call these instead of the normal asserts, like e.g.:
//
// with (context) {
//      val status: String = ...
//      assertEquals("good", status)
// }
//
// ...would call
//     context.assertEquals(expected = "good", message = status)
// which would call
//     assertEquals(expected = "good", actual = context, message = status)

fun <T> T.mustEqual(expected: T, message: String? = null) {
    assertEquals(expected, this, message)
}

fun <T> T.mustMatch(pattern: Regex) =
    toString().let { str ->
        if (!pattern.containsMatchIn(str)) {
            throw AssertionError("Actual value doesn't match $pattern: $str")
        }
    }

inline fun <T> expectFailureOfUnfinished(block: () -> T) {
    try {
        block()
        throw RuntimeException("Expected failure, but passed")
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}
