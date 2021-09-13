package org.jbali.io

import java.io.InputStream
import java.io.OutputStream

/**
 * Copy the entire contents of this stream to the one returned by [outputOpener].
 * This is always closes the input stream, including if [outputOpener] throws, and
 * closes the output stream if it was opened.
 * @return the number of bytes copied.
 */
fun InputStream.copyTo(outputOpener: () -> OutputStream): Long =
    use { ins ->
        outputOpener().use { ous ->
            ins.copyTo(ous)
        }
    }
