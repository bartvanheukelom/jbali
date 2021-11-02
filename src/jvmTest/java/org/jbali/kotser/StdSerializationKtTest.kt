package org.jbali.kotser

import kotlinx.serialization.builtins.serializer
import org.jbali.test.assertContains
import org.junit.Test
import kotlin.test.assertFailsWith

class StdSerializationKtTest {
    @Test fun testParseDiag() {
        // attempt parsing an array as a string
        assertFailsWith<IllegalArgumentException> {
            DefaultJson.plain.parseDiag(
                    String.serializer(),
                    "[123123]"
            )
        }.also { e ->
            assertContains(
                    "kotlinx.serialization.json.internal.JsonDecodingException: Unexpected JSON token at offset 0: Expected beginning of the string, but got [",
                    e.toString()
            )
        }
    }
}