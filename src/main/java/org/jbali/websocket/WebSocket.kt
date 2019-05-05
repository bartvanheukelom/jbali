package org.jbali.websocket

import org.jbali.errors.causeChain
import org.slf4j.LoggerFactory
import java.io.*
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private val log = LoggerFactory.getLogger(WebSocket::class.java)

/**
 * A PacketSocket implementation on top of a WebSocket.
 * @author Bart van Heukelom
 */
class WebSocket(
        val serverMode: Boolean,
        val remoteAddress: InetAddress,
        val backend: Socket,
        inss: InputStream,
        ouss: OutputStream,
        val maxInSize: Int = Integer.MAX_VALUE
) {

    class CloseFrameException(m: String) : RuntimeException(m)
    class ClosedException(val cd: CloseData) : RuntimeException("WebSocket already closed ${cd.reason}")

    enum class CloseReason {
        LOCALLY,
        REMOTELY,
        EOF,
        ERROR
    }

    data class CloseData(
            val reason: CloseReason,
            val exception: Throwable?,
            val extra: Any?
    )

    private val ins = DataInputStream(inss)
    private val ous = DataOutputStream(ouss)

    private val readLock = Any()
    private val writeLock = Any()

    @Volatile
    private var closeData: CloseData? = null
    private val closeFrameSent = AtomicBoolean()
    private val pingsReceived = AtomicInteger()
    private val pongsReceived = AtomicInteger()

    val numberOfPingsReceived: Int get() = pingsReceived.get()
    val numberOfPongsReceived: Int get() = pongsReceived.get()

    val isOpen: Boolean
        get() = !backend.isClosed

    override fun toString(): String {
        return "[WebPacketSocket($backend)]"
    }

    fun toCompactString(): String {
        return "ws:$remoteAddress"
    }

    fun read(): WebSockets.Message {
        checkNotClosed()
        try {
            val readMsg =
                    synchronized(readLock) {
                        WebSockets.readMessage(
                                ins, serverMode,
                                sendReply = { frame ->

                                    if (frame.opcode == WebSockets.OPCODE_PONG) {
                                        pingsReceived.incrementAndGet()
                                    }

                                    val send = frame.opcode != WebSockets.OPCODE_CLOSE || closeFrameSent.compareAndSet(false, true)

                                    if (!send) {
                                        log.debug("Won't echo close, was already sent earlier")
                                    } else {
                                        try {
                                            sendFrame(frame)
                                        } catch (e: Throwable) {
                                            if (frame.opcode == WebSockets.OPCODE_CLOSE &&
                                                    e is SocketException && e.message!!.startsWith("Broken pipe")) {
                                                // client already closed the socket, rude! but doesn't matter then
                                            } else {
                                                log.debug("Error sending reply frame $frame", e)
                                            }
                                        }

                                    }
                                    Unit
                                },
                                onPong = {
                                    pongsReceived.incrementAndGet()
                                    Unit
                                },
                                strictMode = true,
                                maxInSize = maxInSize
                        )
                    }

            return when (readMsg) {
                is WebSockets.ReadMessageResult.Close -> throw CloseFrameException("Close frame received with code ${readMsg.code} and msg ${readMsg.msg}")
                is WebSockets.ReadMessageResult.Actual -> readMsg.msg
            }

        } catch (e: Throwable) {

            // close socket and set close data (unless it's already set to CLOSED_LOCALLY)
            if (closeData == null) {

                try {
                    backend.close()
                } catch (ec: IOException) {
                }

                val cause =
                        when {
                            e is CloseFrameException -> CloseReason.REMOTELY
                            e.causeChain.any { it is EOFException } -> CloseReason.EOF
                            else -> {
                                log.debug("Error reading websocket", e)
                                CloseReason.ERROR
                            }
                        }

                closeData = CloseData(cause, e, null)
            }

            throw ClosedException(closeData!!)

        }

    }

    fun write(message: WebSockets.Message) {
        checkNotClosed()

        try {
            sendFrame(WebSockets.Frame(true, message.opcode, message.asPayload, !serverMode))
        } catch (e: IOException) {
            // close socket and set close data (unless it's already set to CLOSED_LOCALLY)
            if (closeData == null) {
                try {
                    backend.close()
                } catch (ec: IOException) {
                }

                closeData = CloseData(CloseReason.ERROR, e, null)
            }
            throw ClosedException(closeData!!)
        }

    }

    /**
     * Accessible for testing only
     */
    @Suppress("MemberVisibilityCanBePrivate")
    internal fun sendFrame(frame: WebSockets.Frame) {
        synchronized(writeLock) {
            WebSockets.writeFrame(ous, frame)
            ous.flush()
        }
    }

    fun close(extra: Any?): CloseData {

        if (closeFrameSent.compareAndSet(false, true)) {
            try {
                sendFrame(WebSockets.createCloseFrame(extra.toString(), serverMode))
            } catch (e: Throwable) {
                log.warn("Error while sending close frame", e)
            }

        } else {
            log.info("Close frame already sent while closing with $extra")
        }

        checkNotClosed()

        closeData = CloseData(CloseReason.LOCALLY, null, extra)

        try {
            backend.close()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        return closeData!!
    }

    private fun checkNotClosed() {
        if (closeData != null) throw ClosedException(closeData!!)
    }
}
