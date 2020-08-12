package org.jbali.kotser

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.JsonElement
import org.jbali.json2.JSONString
import kotlin.test.assertEquals

// TODO contribute to kotlinserialization lib

/**
 * Asserts that the given [JSONString]s are equal, after normalizing the formatting.
 */
fun assertJsonEquals(expected: JSONString, actual: JSONString, message: String? = null) {
    assertEquals(expected.prettify(), actual.prettify(), message)
}

/**
 * Assert that [obj] serializes to [expectJson], after normalization,
 * and that the result of parsing said JSON equals [obj].
 * Returns the reparsed object.
 */
fun <T> JsonSerializer<T>.assertSerialization(obj: T, expectJson: JSONString): T {

    val actualJson: JSONString = stringify(obj)
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


@ImplicitReflectionSerializer
inline infix fun <reified T> T.shouldSerializeTo(expectJson: JsonElement): T =
        jsonSerializer<T>().assertSerialization(this, expectJson)
