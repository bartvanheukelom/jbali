package org.jbali.io

import org.jbali.bytes.BinaryData
import org.jbali.events.MutableObservable
import org.jbali.events.Observable
import org.jbali.sched.GlobalScheduler
import org.jbali.sched.ScheduledTask
import org.jbali.sched.Scheduler
import org.jbali.util.logger
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Monitors a file and exposes its contents as an [org.jbali.events.Observable] of [org.jbali.bytes.BinaryData].
 *
 * This class watches the file system for changes and optionally polls the file at regular intervals
 * to detect modifications. When changes are detected, the file is re-read and the new content is
 * dispatched to observers.
 *
 * **Content semantics:**
 * - `null` indicates the file does not exist
 * - Non-null [org.jbali.bytes.BinaryData] contains the complete file contents
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
            Scheduler.TaskToSchedule(
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