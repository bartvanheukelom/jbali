package org.jbali.runtime

import org.jbali.bytes.BinaryData
import org.jbali.events.ListenerReference
import org.jbali.io.MonitoredFileReader
import org.jbali.util.logger
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Duration
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Automatically synchronizes system properties from a monitored properties file.
 *
 * This class combines [org.jbali.io.MonitoredFileReader] and [SystemPropertyRuntimeUpdater] to provide
 * automatic, continuous synchronization between a properties file and [System.getProperties].
 *
 * **Behavior:**
 * - Watches the specified file for changes (file system events + optional polling)
 * - When the file changes, parses it as a Java properties file
 * - Updates system properties to match the file contents
 * - When properties are removed from the file, restores their original system values
 * - When the file is deleted, restores all properties to their original values
 *
 * **Partial write protection:**
 * - Set [requireEOF] to `true` to require an `EOF=1` property at the end of the file
 * - If [requireEOF] is true and `EOF=1` is not present, the file is ignored (logged but not applied)
 * - The `EOF=1` property itself is not added to system properties
 * - Use this to ensure only complete writes are processed (writer must append `EOF=1` as last property)
 *
 * **Initialization:**
 * - If [initComplete] is `true` (default), the constructor blocks until the file is successfully read
 * - If the file doesn't exist initially, the constructor returns immediately
 * - If [initComplete] is `false`, initialization happens asynchronously
 *
 * **Resource management:**
 * - Implements [AutoCloseable] - must be closed to release resources
 * - Closing stops file monitoring and does NOT restore original system property values
 *
 * **Thread safety:**
 * - All operations are thread-safe
 * - File changes are processed sequentially
 *
 * **Example file format:**
 * ```properties
 * # My application config
 * app.name=MyApp
 * app.version=1.0
 * logging.level=DEBUG
 * EOF=1
 * ```
 *
 * @param file The properties file to monitor. Need not exist initially.
 * @param requireEOF If `true`, only process files that contain `EOF=1` as a property.
 *        Defaults to `true` for safer operation.
 * @param initComplete If `true`, block the constructor until the initial file read completes (if file exists).
 *        Defaults to `true` to ensure properties are loaded before application startup proceeds.
 * @param pollInterval Interval for polling the file in addition to file system events.
 *        Defaults to 10 seconds. Set to `null` to disable polling (file system events only).
 *
 * @see org.jbali.io.MonitoredFileReader
 * @see SystemPropertyRuntimeUpdater
 */
class SystemPropertyFileRuntimeUpdater(
    private val file: File,
    private val requireEOF: Boolean = true,
    initComplete: Boolean = true,
    pollInterval: Duration? = Duration.ofSeconds(10),
) : AutoCloseable {

    private val log = logger<SystemPropertyFileRuntimeUpdater>()

    private val reader = MonitoredFileReader(file, pollInterval)
    private val updater = SystemPropertyRuntimeUpdater("file:${file.absolutePath}")

    private val initLatch = if (initComplete && file.exists()) CountDownLatch(1) else null
    private val listener: ListenerReference

    init {
        listener = reader.contents.listen { binaryData ->
            processFileContents(binaryData)
            initLatch?.countDown()
        }

        // Wait for initial read if requested and file exists
        initLatch?.let {
            try {
                if (!it.await(30, TimeUnit.SECONDS)) {
                    log.warn("Timeout waiting for initial read of: ${file.absolutePath}")
                }
            } catch (e: InterruptedException) {
                log.warn("Interrupted while waiting for initial read of: ${file.absolutePath}")
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun processFileContents(binaryData: BinaryData?) {
        try {
            val props = when {
                binaryData == null -> {
                    // File doesn't exist - clear all properties
                    log.debug("File does not exist, clearing properties: ${file.absolutePath}")
                    emptyMap()
                }
                else -> {
                    // Parse properties file
                    val properties = Properties()
                    ByteArrayInputStream(binaryData.data).use { inputStream ->
                        properties.load(inputStream)
                    }

                    // Check EOF requirement
                    if (requireEOF) {
                        val eofValue = properties.getProperty("EOF")
                        if (eofValue != "1") {
                            log.warn("File does not contain EOF=1, ignoring: ${file.absolutePath}")
                            return
                        }
                        // Remove EOF property - don't set it as a system property
                        properties.remove("EOF")
                    }

                    // Convert to Map<String, String>
                    properties.stringPropertyNames().associateWith { properties.getProperty(it) }
                }
            }

            // Update system properties
            updater.update(props)

        } catch (e: Exception) {
            log.error("Error processing properties file: ${file.absolutePath}", e)
        }
    }

    override fun close() {
        listener.detach()
        reader.close()
    }

}