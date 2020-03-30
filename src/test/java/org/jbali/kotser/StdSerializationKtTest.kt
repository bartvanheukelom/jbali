package org.jbali.kotser

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.serializer
import org.jbali.test.assertContains
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

class StdSerializationKtTest {
    @Test fun testParseDiag() {
        // attempt parsing an array as a string
        assertFailsWith<IllegalArgumentException> {
            Json(JsonConfiguration.Stable).parseDiag(
                    String.serializer(),
                    "[123123]"
            )
        }.also { e ->
            assertContains(
                    "kotlinx.serialization.json.JsonDecodingException: Unexpected JSON token at offset 0: Expected string literal with quotes",
                    e.toString()
            )
        }
    }
}