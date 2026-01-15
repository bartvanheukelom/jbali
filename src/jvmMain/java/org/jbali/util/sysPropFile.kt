package org.jbali.util

import org.jbali.bytes.BinaryData
import org.jbali.events.ListenerReference
import org.jbali.events.MutableObservable
import org.jbali.events.Observable
import org.jbali.sched.GlobalScheduler
import org.jbali.sched.ScheduledTask
import org.jbali.sched.Scheduler.TaskToSchedule
import org.jbali.util.logger
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.*
import java.time.Duration
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Monitors a file and exposes its contents as an [Observable] of [BinaryData].
 *
 * This class watches the file system for changes and optionally polls the file at regular intervals
 * to detect modifications. When changes are detected, the file is re-read and the new content is
 * dispatched to observers.
 *
 * **Content semantics:**
 * - `null` indicates the file does not exist
 * - Non-null [BinaryData] contains the complete file contents
 *
 * **Error handling:**
 * - Read errors (I/O exceptions, permission denied, etc.) are logged but do not affect the observable state
 * - The observable retains its previous value when a read error occurs
 *
 * **Partial write mitigation:**
 * - A small delay (100ms) is applied after detecting changes to allow writes to complete
 * - For stronger guarantees against partial reads, use content-based validation (e.g., checksums, EOF markers)
 *
 * **Thread safety:**
 * - All operations are thread-safe
 * - The observable can be subscribed to from multiple threads
 *
 * **Resource management:**
 * - Implements [AutoCloseable] - must be closed to release resources
 * - Closing stops file watching, cancels polling tasks, and detaches all listeners
 *
 * @param file The file to monitor. Need not exist initially.
 * @param pollInterval If non-null, the file is re-read at this interval in addition to file system events.
 *        Use this as a fallback when file system events may be unreliable. Defaults to 10 seconds.
 *
 * @see contents The observable that emits file contents
 */
class MonitoredFileReader(
    private val file: File,
    private val pollInterval: Duration? = Duration.ofSeconds(10),
) : AutoCloseable {

    private val log = logger<MonitoredFileReader>()

    private val mContents: MutableObservable<BinaryData?> = MutableObservable(null, "MonitoredFileReader(${file.path})")

    /**
     * Observable that emits the current file contents.
     * - Emits `null` when the file does not exist
     * - Emits [BinaryData] containing the complete file bytes when the file exists and is readable
     * - Updates automatically when the file changes (via file system events and/or polling)
     */
    val contents: Observable<BinaryData?> get() = mContents

    private val closed = AtomicBoolean(false)
    private var watchService: WatchService? = null
    private var watchKey: WatchKey? = null
    private var pollingTask: ScheduledTask? = null
    private var watchThread: Thread? = null

    init {
        // Perform initial read
        readAndUpdate()

        // Set up file system watching
        setupWatcher()

        // Set up polling if requested
        pollInterval?.let { interval ->
            scheduleRecurringPoll(interval)
        }
    }

    private fun scheduleRecurringPoll(interval: Duration) {
        pollingTask = GlobalScheduler.schedule(
            TaskToSchedule(
                delay = interval,
                body = {
                    if (!closed.get()) {
                        readAndUpdate()
                        // Reschedule if not closed
                        if (!closed.get()) {
                            scheduleRecurringPoll(interval)
                        }
                    }
                },
                name = "MonitoredFileReader.poll(${file.path})"
            )
        )
    }

    private fun setupWatcher() {
        try {
            val parent = file.parentFile ?: return
            if (!parent.exists()) {
                log.debug("Parent directory does not exist, skipping file watcher: ${parent.path}")
                return
            }

            watchService = FileSystems.getDefault().newWatchService()
            val parentPath = parent.toPath()
            watchKey = parentPath.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
            )

            // Start watch thread
            watchThread = Thread({
                try {
                    while (!closed.get()) {
                        val key = watchService?.poll(1, TimeUnit.SECONDS) ?: continue

                        for (event in key.pollEvents()) {
                            val context = event.context()
                            if (context is Path && context.toString() == file.name) {
                                // Delay slightly to allow write to complete
                                Thread.sleep(100)
                                readAndUpdate()
                            }
                        }

                        if (!key.reset()) {
                            log.warn("Watch key no longer valid for: ${file.path}")
                            break
                        }
                    }
                } catch (e: InterruptedException) {
                    log.debug("Watch thread interrupted for: ${file.path}")
                } catch (e: Exception) {
                    if (!closed.get()) {
                        log.error("Error in file watch thread for: ${file.path}", e)
                    }
                }
            }, "MonitoredFileReader.watch(${file.path})")

            watchThread?.isDaemon = true
            watchThread?.start()

        } catch (e: Exception) {
            log.warn("Failed to set up file watcher for: ${file.path}", e)
        }
    }

    private fun readAndUpdate() {
        try {
            val newContents = if (file.exists()) {
                BinaryData(file.readBytes())
            } else {
                null
            }
            mContents.value = newContents
        } catch (e: Exception) {
            log.error("Error reading file: ${file.path}", e)
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            // Cancel polling task
            pollingTask?.cancel()

            // Stop watch service
            watchKey?.cancel()
            watchThread?.interrupt()
            try {
                watchService?.close()
            } catch (e: Exception) {
                log.warn("Error closing watch service for: ${file.path}", e)
            }

            // Clean up observable
            mContents.destroy()
        }
    }

}


/**
 * Manages runtime updates to system properties with ownership tracking and automatic restoration.
 *
 * This class provides safe, auditable updates to [System.getProperties]. It "owns" the properties it sets
 * and can restore them to their original values when they're removed from subsequent updates.
 *
 * **Value handling:**
 * - Supports any property value type (String, Integer, Boolean, custom objects, etc.)
 * - Distinguishes between `null` values (key present, value is null) and absent keys
 * - Values are compared using `==` for equality
 * - Non-string values in existing properties are preserved and can be restored to their original type
 *
 * **Ownership model:**
 * - When a property is set via [update], this class takes ownership and records:
 *   - Whether the key was originally present
 *   - The original value (which may be any Object type, or null)
 *   - The new value that was set
 * - When a previously-owned property is absent from a subsequent [update]:
 *   - If the current system value equals our set value → restore original state (value or absent)
 *   - If the current system value differs → log a warning (external modification detected) and release ownership
 *
 * **Logging:**
 * - All property additions, modifications, and restorations are logged at INFO level
 * - External modifications (when detected during restoration) are logged at WARN level
 * - The [name] parameter is included in all log messages for traceability
 *
 * **Thread safety:**
 * - Not thread-safe. Callers must synchronize if calling [update] from multiple threads.
 *
 * **Example usage:**
 * ```kotlin
 * val updater = SystemPropertyRuntimeUpdater("config.properties")
 * updater.update(mapOf("foo" to "bar", "baz" to 123))  // Sets foo=bar, baz=123
 * updater.update(mapOf("foo" to "modified"))            // Updates foo, restores baz to original
 * ```
 *
 * @param name Optional identifier for this property source, used in logging (e.g., "file:/etc/app.properties").
 */
class SystemPropertyRuntimeUpdater(
    val name: String? = null,
) {

    private val log = logger<SystemPropertyRuntimeUpdater>()

    private data class OwnedProperty(
        val originalKeyPresent: Boolean,
        val originalValue: Any?,
        val currentValue: Any?
    )

    private val ownedProperties = mutableMapOf<String, OwnedProperty>()

    private val sourceName get() = name?.let { " from $it" } ?: ""

    /**
     * Updates system properties to match the given [props] map.
     *
     * This method:
     * 1. Sets or updates properties present in [props] (taking ownership if not already owned)
     * 2. Restores properties that were previously owned but are now absent from [props]
     *
     * All changes are logged with details about previous and new values.
     *
     * @param props The desired system properties. Keys are property names, values can be any type (or null).
     */
    fun update(props: Map<String, Any?>) {
        val sysProps = System.getProperties()

        // Handle new and updated properties
        for ((key, newValue) in props) {
            val currentSystemValue = sysProps[key]
            val owned = ownedProperties[key]

            when {
                owned == null -> {
                    // New property - take ownership
                    sysProps[key] = newValue
                    ownedProperties[key] = OwnedProperty(
                        originalKeyPresent = sysProps.containsKey(key),
                        originalValue = currentSystemValue,
                        currentValue = newValue
                    )
                    if (!sysProps.containsKey(key) || currentSystemValue == null) {
                        log.info("Set system property$sourceName: $key=$newValue")
                    } else {
                        log.info("Override system property$sourceName: $key: $currentSystemValue → $newValue")
                    }
                }
                owned.currentValue != newValue -> {
                    // Owned property with new value
                    if (currentSystemValue != owned.currentValue) {
                        log.warn("System property $key was externally modified$sourceName: " +
                                "expected=${owned.currentValue}, actual=$currentSystemValue, setting to $newValue")
                    }
                    sysProps[key] = newValue
                    ownedProperties[key] = owned.copy(currentValue = newValue)
                    log.info("Update system property$sourceName: $key: ${owned.currentValue} → $newValue")
                }
                // else: owned.currentValue == newValue, no change needed
            }
        }

        // Handle removed properties (restore originals)
        val removedKeys = ownedProperties.keys - props.keys
        for (key in removedKeys) {
            val owned = ownedProperties[key]!!
            val currentSystemValue = sysProps[key]

            if (currentSystemValue == owned.currentValue) {
                // Our value is still in place - restore original
                if (!owned.originalKeyPresent) {
                    sysProps.remove(key)
                    log.info("Restore system property$sourceName: $key removed (was ${owned.currentValue})")
                } else {
                    sysProps[key] = owned.originalValue
                    log.info("Restore system property$sourceName: $key: ${owned.currentValue} → ${owned.originalValue}")
                }
            } else {
                // Value was modified externally - just release ownership
                log.warn("System property $key was externally modified$sourceName: " +
                        "expected=${owned.currentValue}, actual=$currentSystemValue, releasing ownership without restoration")
            }
            ownedProperties.remove(key)
        }
    }

}


/**
 * Automatically synchronizes system properties from a monitored properties file.
 *
 * This class combines [MonitoredFileReader] and [SystemPropertyRuntimeUpdater] to provide
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
 * @see MonitoredFileReader
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
