package org.jbali.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or

fun uuid5(
    namespace: UUID,
    name: ByteArray,
): UUID {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(namespace.toBytes())
    md.update(name)
    val hash = md.digest()
    return ByteBuffer.allocate(16).run {
        put(hash, 0, 16)
        put(6, 0x50.toByte() or (get(6) and 0x0F.toByte())) // version 5
        put(8, 0x80.toByte() or (get(8) and 0x3F.toByte())) // IETF variant
        UUID(getLong(0), getLong(8))
    }
}

fun UUID.toBytes(): ByteArray =
    ByteBuffer.allocate(16).run {
        order(ByteOrder.BIG_ENDIAN)
        putLong(0, mostSignificantBits)
        putLong(8, leastSignificantBits)
        array()
    }

fun uuidFromBytes(bytes: ByteArray) =
    ByteBuffer.wrap(bytes).run {
        order(ByteOrder.BIG_ENDIAN)
        UUID(getLong(0), getLong(8))
    }
