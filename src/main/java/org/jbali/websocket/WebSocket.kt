package org.jbali.websocket

import com.google.common.base.Charsets
import com.google.common.base.Preconditions
import org.apache.commons.codec.binary.Base64
import org.apache.http.HttpException
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.apache.http.HttpVersion
import org.apache.http.entity.StringEntity
import org.apache.http.impl.DefaultHttpRequestFactory
import org.apache.http.impl.io.*
import org.apache.http.message.BasicHttpResponse
import org.jbali.bytes.xor
import org.jbali.bytes.xor4
import org.jbali.bytes.xor4ip
import org.slf4j.LoggerFactory
import java.io.*
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.math.min
import kotlin.random.Random

@UseExperimental(ExperimentalUnsignedTypes::class)
object WebSocket {

    private val log = LoggerFactory.getLogger(WebSocket::class.java)

    const val OPCODE_CONTINUATION = 0x0
    const val OPCODE_TEXT = 0x1
    const val OPCODE_BINARY = 0x2
    const val OPCODE_CLOSE = 0x8
    const val OPCODE_PING = 0x9
    const val OPCODE_PONG = 0xA

    const val CON_UPGRADE = "Upgrade"
    const val UPGRADE_WEBSOCKET = "websocket"
    const val WS_VERSION = "13"
    const val HEADER_SEC_WEB_SOCKET_VERSION = "Sec-WebSocket-Version"
    const val HEADER_SEC_WEB_SOCKET_KEY = "Sec-WebSocket-Key"
    const val HEADER_SEC_WEB_SOCKET_ACCEPT = "Sec-WebSocket-Accept"
    val HEADERS_FORWARDED_FOR = setOf("Real-Ip", "X-Real-Ip", "Forwarded", "X-Forwarded-For")
    const val ACCEPT_CONSTANT_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

    data class Frame(
            val fin: Boolean,
            val opcode: Int,
            val payload: ByteArray,
            /** In received frames, indicates whether the data was originally masked,
             *  in frames to send, indicate whether it should be in.
             *  In all cases, the data in this object is not masked.
             */
            val mask: Boolean
    ) {

        // the following are generated because that's required for bytearray apparently

        override fun toString() =
                "Frame(fin=$fin, opcode=$opcode, payload=${payload.size}(${payload.copyOfRange(0, min(payload.size, 16)).toString(StandardCharsets.UTF_8)}) mask=$mask)"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Frame

            if (fin != other.fin) return false
            if (opcode != other.opcode) return false
            if (!payload.contentEquals(other.payload)) return false
            if (mask != other.mask) return false

            return true
        }

        override fun hashCode(): Int {
            var result = fin.hashCode()
            result = 31 * result + opcode
            result = 31 * result + payload.contentHashCode()
            result = 31 * result + mask.hashCode()
            return result
        }
    }

    // ========================================== HANDSHAKING ============================================== //

    @Throws(HttpException::class, IOException::class)
    @JvmStatic
    fun clientHandshake(ins: InputStream, ous: OutputStream, host: String?, uri: String) {

        // prepare for output
        val output = SessionOutputBufferImpl(HttpTransportMetricsImpl(), 8 * 1024)
        output.bind(ous)
        val writer = DefaultHttpRequestWriterFactory.INSTANCE.create(output)

        // send the upgrade request
        val req = DefaultHttpRequestFactory.INSTANCE.newHttpRequest("GET", if (uri.isEmpty()) "/" else uri)
        if (host != null) req.addHeader(HttpHeaders.HOST, host)
        req.addHeader(HttpHeaders.CONNECTION, CON_UPGRADE)
        req.addHeader(HttpHeaders.UPGRADE, UPGRADE_WEBSOCKET)
        req.addHeader(HEADER_SEC_WEB_SOCKET_VERSION, WS_VERSION)
        val key = Base64.encodeBase64String(Random.nextBytes(16))
        req.addHeader(HEADER_SEC_WEB_SOCKET_KEY, key)
        writer.write(req)
        output.flush()

        // prepare for input
        val input = SessionInputBufferImpl(HttpTransportMetricsImpl(), 8 * 1024)
        input.bind(ins)
        val parser = DefaultHttpResponseParserFactory.INSTANCE.create(input, null)

        // read the response
        val resp = parser.parse()
        try {

            // check basic response info
            if (resp.statusLine.statusCode != HttpStatus.SC_SWITCHING_PROTOCOLS)
                throw RuntimeException("Server not switching protocols")
            if (!resp.getFirstHeader(HttpHeaders.CONNECTION).value.equals(CON_UPGRADE, ignoreCase = true))
                throw RuntimeException("Server doesn't upgrade")
            if (!resp.getFirstHeader(HttpHeaders.UPGRADE).value.equals(UPGRADE_WEBSOCKET, ignoreCase = true))
                throw RuntimeException("Server doesn't upgrade to websocket")

            // check the key
            val acceptKey = calcAcceptKey(key)
            val respKey = resp.getFirstHeader(HEADER_SEC_WEB_SOCKET_ACCEPT).value
            if (!respKey.equals(acceptKey, ignoreCase = true))
                throw RuntimeException("Received key '$respKey' doesn't match expected '$acceptKey'")

            // the socket is now a websocket
        } catch (e: Throwable) {
            log.warn("WebSocket response that resulted in exception was:\n$resp")
            throw e
        }

    }

    @Throws(IOException::class, HttpException::class)
    @JvmStatic
    fun serverHandshake(ins: InputStream, ous: OutputStream, remoteAddr: InetAddress): InetAddress {

        //		log.info("Doing WS handshake with " + remoteAddr);

        // prepare for input
        //		log.info("Prepare input");
        val input = SessionInputBufferImpl(HttpTransportMetricsImpl(), 8 * 1024)
        input.bind(ins)

        // parse the request
        //		log.info("Create parser");
        val parser = DefaultHttpRequestParserFactory.INSTANCE.create(input, null)
        //		log.info("Parsing request");
        val req = parser.parse()
        //		log.info("Request: " + req.toString());
        //		System.out.println(req.toString());

        try {

            // prepare for output
            val output = SessionOutputBufferImpl(HttpTransportMetricsImpl(), 8 * 1024)
            output.bind(ous)
            val writer = DefaultHttpResponseWriterFactory.INSTANCE.create(output)

            try {

                // TODO should require Connection: Upgrade, and Sec-WebSocket-Version: 13

                val upgradeHeader = req.getFirstHeader(HttpHeaders.UPGRADE)
                Preconditions.checkNotNull(upgradeHeader, "Required 'Upgrade: websocket'. Upgrade header missing.")
                val upgradeHeaderVal = upgradeHeader.value
                if (!upgradeHeaderVal.equals(UPGRADE_WEBSOCKET, ignoreCase = true))
                    throw IllegalArgumentException("Required 'Upgrade: websocket'. Given: $upgradeHeaderVal")

                // determine the real remote address
                // TODO should be a setting whether to trust this
                var remoteAddress = remoteAddr
                for (fwdHeader in HEADERS_FORWARDED_FOR) {
                    val xff = req.getFirstHeader(fwdHeader)
                    if (xff != null) {
                        remoteAddress = InetAddress.getByName(xff.value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].trim { it <= ' ' })
                        break
                    }
                }

                val acceptKey = calcAcceptKey(req.getFirstHeader(HEADER_SEC_WEB_SOCKET_KEY).value)

                val resp = BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_SWITCHING_PROTOCOLS, "Switching Protocols")
                resp.addHeader(HttpHeaders.CONNECTION, CON_UPGRADE)
                resp.addHeader(HttpHeaders.UPGRADE, UPGRADE_WEBSOCKET)
                resp.addHeader(HEADER_SEC_WEB_SOCKET_ACCEPT, acceptKey)
                writer.write(resp)
                output.flush()
                ous.flush()

                return remoteAddress

            } catch (e: Throwable) {
                val resp = BasicHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_BAD_REQUEST, "Bad Request")
                resp.entity = StringEntity(e.toString())
                writer.write(resp)
                output.flush()
                // TODO this is sent (as seen with netcat) but browser/curl doesn't accept it. Content-length is not sent, may be the issue?
                resp.entity.writeTo(ous)
                ous.flush()
                throw RuntimeException("Websocket handshake error: $e", e)
            }

        } catch (e: Throwable) {
            log.warn("WebSocket request that resulted in exception was:\n$req")
            throw e
        }

    }


    private fun calcAcceptKey(inKey: String): String {

        val sha: MessageDigest
        try {
            sha = MessageDigest.getInstance("SHA-1")
        } catch (e: NoSuchAlgorithmException) {
            throw AssertionError()
        }

        val concat = inKey + ACCEPT_CONSTANT_GUID
        val hash = sha.digest(concat.toByteArray(Charsets.US_ASCII))

        return Base64.encodeBase64String(hash)

    }


    // ====================================================== FRAMES ================================================= //

    @JvmStatic
    fun writeFrame(ous: DataOutput, frame: Frame) {

        val (fin, opcode, payload, mask) = frame
        val payloadSize = payload.size

        // --- write FIN and opcode

        var h1 = 0

        // check if opcode is valid
        Preconditions.checkArgument(opcode and 15 == opcode)

        if (fin) h1 = h1 or 128
        h1 = h1 or opcode

        ous.writeByte(h1)

        // --- write length

        val len1: Int
        if (payloadSize <= 125) {
            // length can be stored in 1 byte
            len1 = payloadSize
        } else {
            // length must be stored in...
            if (payloadSize <= 0xFFFF) {
                len1 = 126 // 2 bytes...
            } else {
                len1 = 127 // or 4 bytes
            }
        }
        var h2 = len1
        if (mask) h2 = h2 or 128
        ous.writeByte(h2)

        if (len1 >= 126) {
            if (len1 == 126)
                ous.writeShort(payloadSize)
            else
                ous.writeLong(payloadSize.toLong())
        }

        // --- write payload

        if (!mask) {
            ous.write(payload)
        } else {

            // generate a 4 byte masking key and write it out
            val maskingKey = Random.nextBytes(4)
            ous.write(maskingKey)

            // --- now xor that key, repeated, with the payload ---

            // compute the part of the payload that is divisible by 4
            val rest = payloadSize % 4
            val aligned = payloadSize - rest
            // try to efficiently mask and write that part
            var b = 0
            while (b < aligned) {
                ous.write(payload.xor4(b, maskingKey))
                b += 4
            }
            // do the rest a little bit slower
            if (rest > 0) {
                val restSlice = payload.copyOfRange(aligned, payloadSize)
                ous.write(restSlice.xor(maskingKey))
            }
        }

    }

    @Throws(IOException::class)
    @JvmStatic
    fun readFrame(ins: DataInput, maxInSize: Int): Frame {

        val h1 = ins.readUnsignedByte()

        //		System.out.println(Integer.toString(h1, 2));
        val fin = h1 and 128 != 0
        //		System.out.println("fin " + fin);

        val extended = h1 and 112 != 0
        if (extended) throw IllegalArgumentException("Unsupported extension")

        val opcode = h1 and 15
        //		System.out.println("opcode " + opcode);

        val h2 = ins.readUnsignedByte()

        val masked = h2 and 128 != 0
        //		System.out.println("masked " + masked);

        val payloadSize: Int

        val len1 = h2 and 127
        //		System.out.println("len1 " + len1);
        if (len1 == 126) {
            payloadSize = ins.readUnsignedShort()
        } else if (len1 == 127) {
            val l = ins.readLong()
            if (l < 0 || l > Integer.MAX_VALUE) throw IllegalArgumentException()
            payloadSize = l.toInt()
        } else {
            payloadSize = len1
        }

        // for security, don't event attempt to read very large frames
        if (payloadSize > maxInSize)
            throw IllegalArgumentException("Incoming frame length $payloadSize is larger than limit $maxInSize")

        val maskingKey =
                if (!masked) null else ByteArray(4).also { ins.readFully(it) }

        val payload = ByteArray(payloadSize).also { ins.readFully(it) }

        if (maskingKey != null) {

            // compute the part of the payload that is divisible by 4
            val rest = payloadSize % 4
            val aligned = payloadSize - rest
            // try to efficiently mask and write that part
            var b = 0
            while (b < aligned) {
                payload.xor4ip(b, maskingKey)
                b += 4
            }
            // do the rest a little bit slower
            if (rest > 0) {
                payload
                        .copyOfRange(aligned, payloadSize)
                        .xor(maskingKey)
                        .copyInto(payload, aligned)
            }
        }

        return Frame(fin, opcode, payload, masked)
    }

    private enum class ReadingPack {
        NONE, BINARY, TEXT
    }

    sealed class ReadMessage {
        class Binary(val data: ByteArray) : ReadMessage()
        class Text(val data: String) : ReadMessage()
        data class Close(val code: UShort?, val msg: String?) : ReadMessage()
    }

    @JvmStatic
    fun readMessage(ins: DataInput, serverMode: Boolean,
                    sendReply: (Frame) -> Unit,
                    onPong: () -> Unit = {},
                    strictMode: Boolean = true, maxInSize: Int = 2_000_000): ReadMessage {

        val pack = ByteArrayOutputStream()
        var rp = ReadingPack.NONE

        // receive a packet, which can consist of multiple frames
        loop@ while (true) {

            val (fin, opcode, data, wasMasked) = readFrame(ins, maxInSize)

            if (serverMode && strictMode && wasMasked) {
                sendReply(createCloseFrame(1002u, "must use masking", !serverMode))
                throw IllegalArgumentException("Received unmasked frame in server mode")
            }

            when (opcode) {

                OPCODE_CLOSE -> {

                    // client is closing the connection

                    // parse the extra data from the close frame, if possible
                    val closeCodeIn: UShort?
                    val closeMsgIn: String?
                    val closeCodeOut: UShort
                    val closeMessageOut: String
                    try {
                        if (data.size >= 2) {
                            val b = ByteBuffer.wrap(data)
                            closeCodeIn = b.short.toUShort()
                            closeMsgIn = StandardCharsets.UTF_8.decode(b.slice()).toString()
                            closeCodeOut = closeCodeIn
                            closeMessageOut = "Echo: $closeMsgIn"
                        } else {
                            closeCodeIn = null
                            closeMsgIn = null
                            closeCodeOut = 0u
                            closeMessageOut = "Close received without payload"
                        }
                    } catch (e: Throwable) {
                        throw RuntimeException("Close frame received, but error in handling it: $e", e)
                    }

                    // write an echo close frame, as required
                    sendReply(createCloseFrame(closeCodeOut, closeMessageOut, serverMode))
                    return ReadMessage.Close(closeCodeIn, closeMsgIn)
                }

                OPCODE_PING -> sendReply(Frame(false, OPCODE_PONG, data, !serverMode))
                OPCODE_PONG -> onPong()

                OPCODE_CONTINUATION -> {
                    if (rp == ReadingPack.NONE) {
                        sendReply(createCloseFrame(1002u, "unexpected continuation", !serverMode))
                        throw IllegalStateException("Got continuation frame while expecting opening frame")
                    }
                    pack.write(data)
                    if (fin) break@loop
                }
                OPCODE_TEXT, OPCODE_BINARY -> {
                    if (rp != ReadingPack.NONE) {
                        sendReply(createCloseFrame(1002u, "unexpected new message", !serverMode))
                        throw IllegalStateException("Got frame with opcode $opcode while expecting continuations")
                    }
                    rp = if (opcode == OPCODE_BINARY) ReadingPack.BINARY else ReadingPack.TEXT
                    pack.write(data)
                    if (fin) break@loop
                }

                else -> throw IllegalStateException("Received frame with unknown opcode 0x" + Integer.toHexString(opcode))
            }

        }

        val data = pack.toByteArray()
        return if (rp == ReadingPack.TEXT) {
            ReadMessage.Text(data.toString(StandardCharsets.UTF_8))
        } else {
            ReadMessage.Binary(data)
        }
    }

    @JvmStatic
    fun createCloseFrame(closeMessageOut: String, serverMode: Boolean) =
            createCloseFrame(1000u, closeMessageOut, serverMode)

    /**
     * See https://tools.ietf.org/html/rfc6455#section-7.4 for codes
     */
    fun createCloseFrame(closeCodeOut: UShort = 1000u, closeMessageOut: String, serverMode: Boolean): Frame {
        val cout = ByteArrayOutputStream()
        val dout = DataOutputStream(cout)
        dout.writeShort(closeCodeOut.toInt())
        dout.write(closeMessageOut.toByteArray(StandardCharsets.UTF_8))
        return Frame(true, OPCODE_CLOSE, cout.toByteArray(), !serverMode)
    }

}
