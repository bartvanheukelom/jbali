package org.jbali.sockets

import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

// TODO make the stream wrappers injectable
class DeflateSocket : Socket {

    constructor() : super()
    constructor(host: String, port: Int) : super(host, port)

    private val input by lazy {
//        log.info("Wrapping input of $this")
        super.getInputStream()
                .let { InflaterInputStream(it) }
//                .let { CountingInputStream(it) }
    }
    private val output by lazy {
//        log.info("Wrapping output of $this")
        super.getOutputStream()
//                .let { LoggingOutputStream(it, log.withPrefix("deflated")) }
                .let { DeflaterOutputStream(it, true) }
//                .let { LoggingOutputStream(it, log.withPrefix("buffered")) }
//                .let { BufferedOutputStream(it, 64000) }
//                .let { LoggingOutputStream(it, log.withPrefix("direct")) }
    }

    override fun getInputStream(): InputStream {
        super.getInputStream() // only call for the exceptions it throws
        return input
    }

    override fun getOutputStream(): OutputStream {
        super.getOutputStream() // only call for the exceptions it throws
        return output
    }

    override fun shutdownOutput() {
        output.flush()
        super.shutdownOutput()
    }

//    override fun close() {
//        log.info("Closing $this with ${input.count} bytes read")
//        super.close()
//    }

    override fun toString() =
            "Deflate${super.toString()}";

//    companion object {
//        val log = slog<DeflateSocket>()
//    }
}


class DeflateServerSocket(
        port: Int
) : ServerSocket(port) {

    override fun accept() =
            DeflateSocket().also { s ->
                implAccept(s)
            }

    override fun toString() =
            "Deflate${super.toString()}"

}
