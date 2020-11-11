package org.jbali.json2

import kotlinx.serialization.builtins.serializer
import org.jbali.kotser.BasicJson
import org.jbali.util.forEachWrappingExceptions
import org.junit.Test
import kotlin.test.assertEquals

class JsonWritingKtTest {

    @Test
    fun testStringJsonQuote() {
        listOf(
                "simple",
                "with space",
                "with, comma",
                "having a \"quote\" inside",
                "and single 'quote' too",
                "{ some kind of fake struct }",
                "[ or array ]",
                "123",
                "123.905",
                "0",
                "true",
                "false",
                "null",
                "is this... \n line two??",
                ""
        ).forEachWrappingExceptions {
            val exp = BasicJson.plain.encodeToString(String.serializer(), it)
            val act = it.jsonQuote()
            println("$it -> $act")
            assertEquals(
                    expected = exp,
                    actual = act
            )
        }
    }

}