package org.jbali.streams

import org.jbali.util.toHexString
import org.slf4j.Logger
import java.io.FilterOutputStream
import java.io.OutputStream

/**
 * [FilterOutputStream] that logs every write and flush to [log]
 * in hexadecimal format. Also counts bytes written.
 */
class LoggingOutputStream(
        out: OutputStream,
        val log: Logger
) : FilterOutputStream(out) {

    private var count: Long = 0

    private fun logg(type: Char, b: ByteArray) {
        count += b.size
        log.info("$type -> ${b.toHexString()} (+${b.size} = $count)")
    }

    override fun write(b: Int) {
        logg('b', byteArrayOf(b.toByte()))
        out.write(b)
    }

    override fun write(b: ByteArray) {
        logg('a', b)
        out.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        logg('s', b.copyOfRange(off, off+len))
        out.write(b, off, len)
    }

    override fun flush() {
        log.info("======= $count ========")
        out.flush()
    }
}