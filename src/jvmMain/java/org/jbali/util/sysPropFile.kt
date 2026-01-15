package org.jbali.util

import org.jbali.bytes.BinaryData
import org.jbali.events.MutableObservable
import org.jbali.events.Observable
import java.io.File
import java.time.Duration
import java.util.Properties


/**
 * Reads the contents of the given file and exposes them as an [Observable] [BinaryData].
 * Uses file watching to read and dispatch updates when the file changes.
 * If [pollInterval] is given, the file will also be re-read at the given interval.
 * null contents indicate that the file does not exist.
 * Other errors while reading are logged but do not affect the observable.
 *
 * Makes an effort to only read the complete file, i.e. not a partial write,
 * but that's hard to guarantee so it's recommended to add your own filtering
 * based on file contents if needed.
 */
class MonitoredFileReader(
    private val file: File,
    private val pollInterval: Duration? = Duration.ofSeconds(10),
) : AutoCloseable {

    private val log = logger<MonitoredFileReader>()

    private val mContents: MutableObservable<BinaryData?> = MutableObservable(null)
    val contents: Observable<BinaryData?> get() = mContents

    override fun close() {
        TODO("Not yet implemented")
    }

}


/**
 * Updates system properties at runtime based on the given [props].
 *
 * Properties set by this class are "owned" by it, and if they are missing from a subsequent [update] call,
 * they are restored to their previous value (if unchanged) or logged as overridden externally.
 *
 * @param name Optional name for the property source, used in logging. Example "file:/path/to/file.properties".
 */
class SystemPropertyRuntimeUpdater(
    val name: String? = null,
) {

    /**
     * Set system properties to the given [props].
     * When a property is added or overriden, this class will "take ownership" of it. The system value, if any
     * is recorded, as well as the new value. In subsequent calls to [update], if the property is missing from [props],
     * it will be restored to its original value (if its system value equals the value set by this class - else log) and marked as no longer owned.
     *
     * Initial property values and any subsequent changes are logged.
     */
    fun update(props: Map<String, String>) {

    }

}


/**
 * Combines [MonitoredFileReader] and [SystemPropertyRuntimeUpdater] to add properties from a file to system properties.
 *
 * @param file The properties file to monitor.
 * @param requireEOF If true, the file contents are only accepted if they define a last property EOF=1, to filter out partial writes.
 * @param initComplete If true, the constructor requires a successful initial read of the file (if it exists) before returning.
 */
class SystemPropertyFileRuntimeUpdater(
    private val file: File,
    requireEOF: Boolean = true,
    initComplete: Boolean = true,
    pollInterval: Duration? = Duration.ofSeconds(10),
) {


}
