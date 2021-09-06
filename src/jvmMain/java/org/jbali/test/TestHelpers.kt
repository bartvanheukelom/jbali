package org.jbali.test

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
