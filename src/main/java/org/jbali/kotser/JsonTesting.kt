package org.jbali.kotser

import kotlinx.serialization.json.JsonElement
import org.jbali.json2.JSONString
import kotlin.test.assertEquals

// TODO contribute to kotlinserialization lib


/**
 * Asserts that the given [JSONString], when parsed, is equal to [expected].
 */
fun assertJsonEquals(expected: JsonElement, actual: JSONString, message: String? = null) {
    assertEquals(
            expected = expected,
            actual = actual.parse(),
            message = message
    )
}


/**
 * Asserts that the given [JSONString]s are equal, after normalizing the formatting.
 */
fun assertJsonEquals(expected: JSONString, actual: JSONString, message: String? = null) {
    assertEquals(
            expected =
                    try {
                        expected.prettify()
                    } catch (e: Exception) {
                        throw IllegalArgumentException("The expected value given to assertJsonEquals appears to be invalid: $e", e)
                    },
            actual = actual.prettify(),
            message = message
    )
}

/**
 * Assert that [obj] serializes to [expectJson], after normalization,
 * and that the result of parsing said JSON equals [obj].
 * Returns the reparsed object.
 */
fun <T> JsonSerializer<T>.assertSerialization(obj: T, expectJson: JSONString): T {

    val actualJson: JSONString = try {
        stringify(obj)
    } catch (e: Throwable) {
        throw AssertionError("Error while serializing $obj: $e", e)
    }
    assertJsonEquals(expectJson, actualJson, "stringify($obj)")

    val back = parse(actualJson)
    assertEquals(obj, back, "parse($actualJson)")

    return back
}

/**
 * Assert that [obj] serializes to [expectJson], after normalization,
 * and that the result of parsing said JSON equals [obj].
 * Returns the reparsed object.
 */
fun <T> JsonSerializer<T>.assertSerialization(obj: T, expectJson: JsonElement): T =
        assertSerialization(obj, JSONString.stringify(expectJson))

fun <T> JsonSerializer<T>.assertSerialization(obj: T, expectJson: String): T =
        assertSerialization(obj, JSONString(expectJson))


inline infix fun <reified T> T.shouldSerializeTo(expectJson: JsonElement): T =
        jsonSerializer<T>().assertSerialization(this, expectJson)
