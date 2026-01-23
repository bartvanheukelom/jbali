package org.jbali.runtime

import kotlinx.coroutines.*
import org.jbali.coroutines.awaitFor
import org.jbali.io.createFreeTempDir
import org.jbali.util.logger
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class SystemPropertyFileRuntimeUpdaterTest {

    private val log = logger<SystemPropertyFileRuntimeUpdaterTest>()

    @BeforeTest
    fun clearTestProperties() {
        // Clear any test properties that might be left over
        System.clearProperty("test.prop1")
        System.clearProperty("test.prop2")
        System.clearProperty("test.prop3")
    }

    @AfterTest
    fun cleanupTestProperties() {
        System.clearProperty("test.prop1")
        System.clearProperty("test.prop2")
        System.clearProperty("test.prop3")
    }

    @Test
    fun testInitCompleteBlocksUntilFileRead() {
        createFreeTempDir(prefix = "sysPropFileUpd").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.properties").toFile()
            testFile.writeText("""
                test.prop1=value1
                test.prop2=value2
                EOF=1
            """.trimIndent())

            // initComplete=true (default) should block until file is read
            SystemPropertyFileRuntimeUpdater(testFile).use { updater ->
                // Properties should be immediately available after constructor returns
                assertEquals("value1", System.getProperty("test.prop1"))
                assertEquals("value2", System.getProperty("test.prop2"))
            }
        }
    }

    @Test
    fun testInitCompleteWithNonExistentFile() {
        createFreeTempDir(prefix = "sysPropFileUpd").use { tempDir ->
            val testFile = tempDir.dir.resolve("nonexistent.properties").toFile()

            // Should not block or throw for non-existent file
            SystemPropertyFileRuntimeUpdater(testFile, initComplete = true).use { updater ->
                assertNull(System.getProperty("test.prop1"))
            }
        }
    }

    @Test
    fun testInitCompleteAsyncInitialization() = runBlocking {
        createFreeTempDir(prefix = "sysPropFileUpd").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.properties").toFile()
            testFile.writeText("""
                test.prop1=async_value
                EOF=1
            """.trimIndent())

            val deferred = CompletableDeferred<Unit>()

            // initComplete=false should return immediately
            SystemPropertyFileRuntimeUpdater(testFile, initComplete = false).use { updater ->
                // Property might not be set yet
                val initialValue = System.getProperty("test.prop1")
                log.info("Initial value: $initialValue")

                // Wait for the async read to complete
                delay(1000)

                assertEquals("async_value", System.getProperty("test.prop1"))
            }
        }
    }

    @Test
    fun testFileModificationUpdatesProperties() = runBlocking {
        createFreeTempDir(prefix = "sysPropFileUpd").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.properties").toFile()
            testFile.writeText("""
                test.prop1=initial
                EOF=1
            """.trimIndent())

            SystemPropertyFileRuntimeUpdater(testFile, pollInterval = java.time.Duration.ofMillis(200)).use { updater ->
                assertEquals("initial", System.getProperty("test.prop1"))

                // Modify file
                delay(200)
                testFile.writeText("""
                    test.prop1=modified
                    EOF=1
                """.trimIndent())

                // Wait for change detection
                delay(1000)

                assertEquals("modified", System.getProperty("test.prop1"))
            }
        }
    }

    @Test
    fun testPropertyAdditionAndRemoval() = runBlocking {
        createFreeTempDir(prefix = "sysPropFileUpd").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.properties").toFile()
            testFile.writeText("""
                test.prop1=value1
                test.prop2=value2
                EOF=1
            """.trimIndent())

            SystemPropertyFileRuntimeUpdater(testFile, pollInterval = java.time.Duration.ofMillis(200)).use { updater ->
                assertEquals("value1", System.getProperty("test.prop1"))
                assertEquals("value2", System.getProperty("test.prop2"))

                // Remove prop2, modify prop1, add prop3
                delay(200)
                testFile.writeText("""
                    test.prop1=modified
                    test.prop3=value3
                    EOF=1
                """.trimIndent())

                delay(1000)

                assertEquals("modified", System.getProperty("test.prop1"))
                assertNull(System.getProperty("test.prop2")) // Should be restored (removed)
                assertEquals("value3", System.getProperty("test.prop3"))
            }
        }
    }

    @Test
    fun testEOFRequirement() = runBlocking {
        createFreeTempDir(prefix = "sysPropFileUpd").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.properties").toFile()

            // Write file without EOF
            testFile.writeText("""
                test.prop1=should_be_ignored
            """.trimIndent())

            SystemPropertyFileRuntimeUpdater(testFile, requireEOF = true, pollInterval = java.time.Duration.ofMillis(200)).use { updater ->
                delay(500)
                // Property should not be set because EOF=1 is missing
                assertNull(System.getProperty("test.prop1"))

                // Add EOF
                testFile.writeText("""
                    test.prop1=should_be_set
                    EOF=1
                """.trimIndent())

                delay(1000)

                // Now property should be set
                assertEquals("should_be_set", System.getProperty("test.prop1"))
            }
        }
    }

    @Test
    fun testEOFNotSetAsProperty() {
        createFreeTempDir(prefix = "sysPropFileUpd").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.properties").toFile()
            testFile.writeText("""
                test.prop1=value1
                EOF=1
            """.trimIndent())

            SystemPropertyFileRuntimeUpdater(testFile, requireEOF = true).use { updater ->
                assertEquals("value1", System.getProperty("test.prop1"))
                // EOF should not be set as a system property
                assertNull(System.getProperty("EOF"))
            }
        }
    }

    @Test
    fun testRequireEOFFalse() {
        createFreeTempDir(prefix = "sysPropFileUpd").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.properties").toFile()
            testFile.writeText("""
                test.prop1=value1
            """.trimIndent())

            SystemPropertyFileRuntimeUpdater(testFile, requireEOF = false).use { updater ->
                // Property should be set even without EOF
                assertEquals("value1", System.getProperty("test.prop1"))
            }
        }
    }

    @Test
    fun testFileDeletionRestoresProperties() = runBlocking {
        createFreeTempDir(prefix = "sysPropFileUpd").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.properties").toFile()

            // Set a system property before starting
            System.setProperty("test.prop1", "original")

            testFile.writeText("""
                test.prop1=overridden
                EOF=1
            """.trimIndent())

            SystemPropertyFileRuntimeUpdater(testFile, pollInterval = java.time.Duration.ofMillis(200)).use { updater ->
                assertEquals("overridden", System.getProperty("test.prop1"))

                // Delete file
                delay(200)
                testFile.delete()

                delay(1000)

                // Property should be restored to original
                assertEquals("original", System.getProperty("test.prop1"))
            }
        }
    }

    @Test
    fun testFileDeletionClearsNewProperties() = runBlocking {
        createFreeTempDir(prefix = "sysPropFileUpd").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.properties").toFile()

            testFile.writeText("""
                test.prop1=value1
                EOF=1
            """.trimIndent())

            SystemPropertyFileRuntimeUpdater(testFile, pollInterval = java.time.Duration.ofMillis(200)).use { updater ->
                assertEquals("value1", System.getProperty("test.prop1"))

                // Delete file
                delay(200)
                testFile.delete()

                delay(1000)

                // Property should be cleared (it didn't exist before)
                assertNull(System.getProperty("test.prop1"))
            }
        }
    }

    @Test
    fun testCloseRestoresProperties() {
        createFreeTempDir(prefix = "sysPropFileUpd").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.properties").toFile()

            // Set original value
            System.setProperty("test.prop1", "original")

            testFile.writeText("""
                test.prop1=overridden
                test.prop2=new_property
                EOF=1
            """.trimIndent())

            val updater = SystemPropertyFileRuntimeUpdater(testFile)
            assertEquals("overridden", System.getProperty("test.prop1"))
            assertEquals("new_property", System.getProperty("test.prop2"))

            updater.close()

            // Properties should be restored after close
            assertEquals("original", System.getProperty("test.prop1"))
            assertNull(System.getProperty("test.prop2"))
        }
    }

    @Test
    fun testEmptyFile() {
        createFreeTempDir(prefix = "sysPropFileUpd").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.properties").toFile()
            testFile.writeText("")

            // Should handle empty file gracefully
            SystemPropertyFileRuntimeUpdater(testFile, requireEOF = false).use { updater ->
                // No properties should be set
                assertNull(System.getProperty("test.prop1"))
            }
        }
    }

    @Test
    fun testPropertiesFileFormat() {
        createFreeTempDir(prefix = "sysPropFileUpd").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.properties").toFile()
            testFile.writeText("""
                # Comment line
                test.prop1=value1
                test.prop2 = value with spaces
                test.prop3=value\nwith\nnewlines
                EOF=1
            """.trimIndent())

            SystemPropertyFileRuntimeUpdater(testFile).use { updater ->
                assertEquals("value1", System.getProperty("test.prop1"))
                assertEquals("value with spaces", System.getProperty("test.prop2"))
                assertEquals("value\nwith\nnewlines", System.getProperty("test.prop3"))
            }
        }
    }
}
