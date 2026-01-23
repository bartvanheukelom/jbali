package org.jbali.io

import kotlinx.coroutines.*
import org.jbali.bytes.BinaryData
import org.jbali.coroutines.awaitFor
import org.jbali.util.logger
import java.io.File
import java.time.Duration
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlin.time.ExperimentalTime::class)
class MonitoredFileReaderTest {

    private val log = logger<MonitoredFileReaderTest>()

    @Test
    fun testInitialReadNonExistentFile() {
        createFreeTempDir(prefix = "monFileReader").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.txt").toFile()

            MonitoredFileReader(testFile).use { reader ->
                assertNull(reader.contents.get(), "Non-existent file should have null contents")
            }
        }
    }

    @Test
    fun testInitialReadExistingFile() {
        createFreeTempDir(prefix = "monFileReader").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.txt").toFile()
            val content = "initial content"
            testFile.writeText(content)

            MonitoredFileReader(testFile).use { reader ->
                val data = reader.contents.get()
                assertNotNull(data)
                assertEquals(content, data.toUtf8String())
            }
        }
    }

    @Test
    fun testFileCreation() {
        createFreeTempDir(prefix = "monFileReader").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.txt").toFile()
            val deferred = CompletableDeferred<BinaryData?>()

            MonitoredFileReader(testFile, pollInterval = Duration.ofMillis(200)).use { reader ->
                // Initially null
                assertNull(reader.contents.get())

                // Listen for changes
                val listener = reader.contents.listen { content ->
                    log.info("Content changed: ${content?.toUtf8String()}")
                    if (content != null) {
                        deferred.complete(content)
                    }
                }

                // Create the file
                val expectedContent = "new file content"
                testFile.writeText(expectedContent)

                // Wait for change detection
                runBlocking {
                    val result = deferred.awaitFor(5.seconds)
                    assertEquals(expectedContent, result?.toUtf8String())
                }

                listener.detach()
            }
        }
    }

    @Test
    fun testFileModification() {
        createFreeTempDir(prefix = "monFileReader").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.txt").toFile()
            testFile.writeText("initial")

            val deferred = CompletableDeferred<Unit>()

            MonitoredFileReader(testFile, pollInterval = Duration.ofMillis(200)).use { reader ->
                // Check initial content
                assertEquals("initial", reader.contents.get()?.toUtf8String())

                val listener = reader.contents.listen { content ->
                    val text = content?.toUtf8String()
                    log.info("Content update: $text")
                    if (text == "modified") {
                        deferred.complete(Unit)
                    }
                }

                // Modify the file
                Thread.sleep(100)
                testFile.writeText("modified")

                // Wait for change detection
                runBlocking {
                    deferred.awaitFor(5.seconds)
                }

                listener.detach()

                assertEquals("modified", reader.contents.get()?.toUtf8String())
            }
        }
    }

    @Test
    fun testFileDeletion() {
        createFreeTempDir(prefix = "monFileReader").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.txt").toFile()
            testFile.writeText("exists")

            val deferred = CompletableDeferred<Unit>()

            MonitoredFileReader(testFile, pollInterval = Duration.ofMillis(200)).use { reader ->
                assertNotNull(reader.contents.get())

                val listener = reader.contents.listen { content ->
                    log.info("Content changed, exists: ${content != null}")
                    if (content == null) {
                        deferred.complete(Unit)
                    }
                }

                // Delete the file
                Thread.sleep(100)
                testFile.delete()

                // Wait for change detection
                runBlocking {
                    deferred.awaitFor(5.seconds)
                }

                assertNull(reader.contents.get(), "Deleted file should have null contents")
                listener.detach()
            }
        }
    }

    @Test
    fun testMultipleModifications() {
        createFreeTempDir(prefix = "monFileReader").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.txt").toFile()
            testFile.writeText("v1")

            val updates = mutableListOf<String?>()
            val targetContent = "v5"
            val deferred = CompletableDeferred<Unit>()

            MonitoredFileReader(testFile, pollInterval = Duration.ofMillis(200)).use { reader ->
                // Check initial
                assertEquals("v1", reader.contents.get()?.toUtf8String())

                val listener = reader.contents.listen { content ->
                    val text = content?.toUtf8String()
                    log.info("Update: $text")
                    updates.add(text)
                    if (text == targetContent) {
                        deferred.complete(Unit)
                    }
                }

                // Make multiple modifications
                Thread.sleep(100)
                testFile.writeText("v2")
                Thread.sleep(150)
                testFile.writeText("v3")
                Thread.sleep(150)
                testFile.writeText("v4")
                Thread.sleep(150)
                testFile.writeText(targetContent)

                // Wait for final change
                runBlocking {
                    deferred.awaitFor(5.seconds)
                }

                listener.detach()

                assertTrue(updates.contains(targetContent), "Should contain $targetContent")
                // We may or may not catch all intermediate versions due to timing
            }
        }
    }

    @Test
    fun testNoPollInterval() {
        createFreeTempDir(prefix = "monFileReader").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.txt").toFile()
            testFile.writeText("initial")

            val deferred = CompletableDeferred<Unit>()

            // No polling, only file system events
            MonitoredFileReader(testFile, pollInterval = null).use { reader ->
                val listener = reader.contents.listen { content ->
                    val text = content?.toUtf8String()
                    log.info("Content: $text")
                    if (text == "modified") {
                        deferred.complete(Unit)
                    }
                }

                Thread.sleep(100)
                testFile.writeText("modified")

                // Should still work via file system events
                runBlocking {
                    deferred.awaitFor(5.seconds)
                }

                listener.detach()
            }
        }
    }

    @Test
    fun testBinaryContent() {
        createFreeTempDir(prefix = "monFileReader").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.bin").toFile()
            val binaryData = byteArrayOf(0x01, 0x02, 0x03, 0xFF.toByte(), 0xFE.toByte())
            testFile.writeBytes(binaryData)

            MonitoredFileReader(testFile).use { reader ->
                val data = reader.contents.get()
                assertNotNull(data)
                assertContentEquals(binaryData, data.data)
            }
        }
    }

    @Test
    fun testMultipleListeners() {
        createFreeTempDir(prefix = "monFileReader").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.txt").toFile()
            testFile.writeText("initial")

            val updates1 = mutableListOf<String?>()
            val updates2 = mutableListOf<String?>()
            val deferred = CompletableDeferred<Unit>()
            var count = 0

            MonitoredFileReader(testFile, pollInterval = Duration.ofMillis(200)).use { reader ->
                // Check initial
                assertEquals("initial", reader.contents.get()?.toUtf8String())

                val listener1 = reader.contents.listen { content ->
                    updates1.add(content?.toUtf8String())
                    if (content?.toUtf8String() == "final") {
                        if (++count == 2) deferred.complete(Unit)
                    }
                }

                val listener2 = reader.contents.listen { content ->
                    updates2.add(content?.toUtf8String())
                    if (content?.toUtf8String() == "final") {
                        if (++count == 2) deferred.complete(Unit)
                    }
                }

                Thread.sleep(100)
                testFile.writeText("final")

                runBlocking {
                    deferred.awaitFor(5.seconds)
                }

                listener1.detach()
                listener2.detach()

                assertTrue(updates1.contains("final"))
                assertTrue(updates2.contains("final"))
            }
        }
    }

    @Test
    fun testCloseCleanup() {
        createFreeTempDir(prefix = "monFileReader").use { tempDir ->
            val testFile = tempDir.dir.resolve("test.txt").toFile()
            testFile.writeText("test")

            val reader = MonitoredFileReader(testFile)
            assertNotNull(reader.contents.get())

            reader.close()

            // After close, no more updates should occur
            var updateCount = 0
            reader.contents.listen { updateCount++ }

            testFile.writeText("modified after close")
            Thread.sleep(500)

            // No updates should be received after close
            assertEquals(0, updateCount, "Should not receive updates after close")
        }
    }

    @Test
    fun testNonExistentParentDirectory() {
        createFreeTempDir(prefix = "monFileReader").use { tempDir ->
            // File in non-existent directory
            val testFile = tempDir.dir.resolve("nonexistent/test.txt").toFile()

            // Should not throw, just not have file system watching
            MonitoredFileReader(testFile, pollInterval = Duration.ofMillis(200)).use { reader ->
                assertNull(reader.contents.get())
            }
        }
    }

    private fun BinaryData.toUtf8String(): String =
        String(data, Charsets.UTF_8)
}
