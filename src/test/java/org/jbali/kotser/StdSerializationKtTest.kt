package org.jbali.kotser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.serializer
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class StdSerializationKtTest {
    @Test fun testParseDiag() {
        try {
            Json(JsonConfiguration.Stable).parseDiag(String.serializer(), "[123123]")
            fail()
        } catch (e: IllegalArgumentException) {
            assertTrue(e.toString().contains("StringSerializer"))
            assertTrue(e.toString().contains("Invalid JSON"))
        }
    }
}