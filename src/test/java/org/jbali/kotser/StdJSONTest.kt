package org.jbali.kotser

import kotlinx.serialization.*
import kotlinx.serialization.builtins.set
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonLiteral
import kotlinx.serialization.json.JsonNull
import org.jbali.kotser.std.InetAddressSerializer
import org.jbali.kotser.std.StdJSON
import org.junit.Test
import java.net.InetAddress
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

inline class JSON(val s: String) {
    fun parse(): JsonElement = StdJSON.indented.parseJson(s)
}

val String.J get() = JSON(this)

@Serializable
data class IPSetHolder(
        val ips: Set<@Serializable(with = InetAddressSerializer::class) InetAddress>
)

class StdJSONTest {

    private val serrer = StdJSON.indented

    @OptIn(ImplicitReflectionSerializer::class)
    private inline fun <reified T : Any> assertJson(json: JSON, v: T, ser: SerializationStrategy<T>? = null) {

        val expectParse: JsonElement = json.parse()
        val actualJson =
                try {
                    JSON(
                            if (ser == null) serrer.stringify(v)
                            else serrer.stringify(ser, v)
                    )
                } catch (e: Throwable) {
                    throw AssertionError("Error stringifying test object $v of type ${v.javaClass}: $e", e)
                }
        val actualReparse: JsonElement = actualJson.parse()

        // this is the important check
        if (expectParse != actualReparse) {
            // if it fails, do this "assertion" that will certainly fail,
            // but that will show the differences prettyPrinted.
            val expectRestringed = JSON(serrer.stringify(expectParse))
            assertEquals(expectRestringed, actualJson)

            throw AssertionError("line before should have thrown")
        }
    }

    @Test
    fun test() {

        // InetAddress
        assertJson("\"1.1.1.1\"".J, InetAddress.getByName("1.1.1.1"))

        // Set<InetAddress>
        assertJson(
                """
                    [
                        "1.1.1.1",
                        "::1"
                    ]
                """.J,
                listOf("1.1.1.1", "::1").map(InetAddress::getByName).toSet(),
                InetAddressSerializer.set
        )

        // class IPSetHolder { ips: Set<InetAddress> }
        assertJson(
                """
                    {
                        "ips": [
                            "1.1.1.1",
                            "::1"
                        ]
                    }
                """.J,
                IPSetHolder(
                    listOf("1.1.1.1", "::1").map(InetAddress::getByName).toSet()
                )
        )

        // Instant
        assertJson("\"1970-01-01T00:00:00Z\"".J, Instant.ofEpochSecond(0))
        assertJson("\"2019-04-26T00:00:00Z\"".J, LocalDate.of(2019, 4, 26).atStartOfDay(ZoneOffset.UTC).toInstant())

        // LocalDate
        assertJson("\"2019-04-26\"".J, LocalDate.of(2019, 4, 26))

    }

    @OptIn(UnstableDefault::class)
    @Test
    fun testLiteral() {
        val j = DefaultJson.plain


        val constructedTrue = JsonLiteral(true)
        val constructedFalse = JsonLiteral(false)
        val constructedNumber = JsonLiteral(12)
        val constructedString = JsonLiteral("foo")
        val constructedNumberString = JsonLiteral("12")
        val constructedTrueString = JsonLiteral("true")


        j.parseJson("null") as JsonNull

        val parsedTrue = j.parseJson("true") as JsonLiteral
        val parsedFalse = j.parseJson("false") as JsonLiteral
        val parsedFalseWeird = j.parseJson("FaLSe") as JsonLiteral
        val parsedTrueString = j.parseJson("\"true\"") as JsonLiteral

        val parsedNumber = j.parseJson("12") as JsonLiteral
        val parsedNumberString = j.parseJson("\"12\"") as JsonLiteral

        val parsedString = j.parseJson("\"foo\"") as JsonLiteral

        val parsedNullString = j.parseJson("\"null\"") as JsonLiteral


        // the following assertions don't test our code, they document JsonLiteral
        assertEquals(constructedTrue, parsedTrue)
        assertEquals(constructedFalse, parsedFalse)
        assertNotEquals(constructedFalse, parsedFalseWeird) // ! the original string is checked
        assertEquals(constructedNumber, parsedNumber) // ! the string representation of the number is checked
        assertEquals(constructedString, parsedString)
        assertEquals(constructedNumberString, parsedNumberString)
        assertEquals(constructedTrueString, parsedTrueString)


        assertEquals(true, constructedTrue.unwrap())
        assertEquals(true, parsedTrue.unwrap())
        assertEquals(false, constructedFalse.unwrap())
        assertEquals(false, parsedFalse.unwrap())
        assertEquals(false, parsedFalseWeird.unwrap())
        assertEquals("true", parsedTrueString.unwrap())

        assertEquals(12, constructedNumber.unwrap())
        assertEquals(12.0, parsedNumber.unwrap())
        assertEquals("12", parsedNumberString.unwrap())

        assertEquals("foo", constructedString.unwrap())
        assertEquals("foo", parsedString.unwrap())

        assertEquals("null", parsedNullString.unwrap())

    }
}