package org.jbali.bytes

import kotlin.experimental.xor


infix fun ByteArray.xor(mask: ByteArray): ByteArray {
    return if (size == 4) {
        check(mask.size >= 4)
        this xor4 mask
    } else {
        check(mask.size > size)
        mapBytes { i, b -> b xor mask[i] }
    }
}

/**
 * XOR this byte array with a mask.
 * They must both be 4 bytes long, but that is not verified by this function!
 */
@Suppress("NOTHING_TO_INLINE")
inline infix fun ByteArray.xor4(mask: ByteArray): ByteArray {
    val res = ByteArray(4)
    res[0] = this[0] xor mask[0]
    res[1] = this[1] xor mask[1]
    res[2] = this[2] xor mask[2]
    res[3] = this[3] xor mask[3]
    return res
}

/**
 * XOR 4 bytes from this byte array, starting at offset, with a mask of 4 bytes long, into a new array.
 * Does not check whether the given parameters are valid!
 */
inline fun ByteArray.xor4(offset: Int, mask: ByteArray): ByteArray {
    val res = ByteArray(4)
    res[0] = this[offset+0] xor mask[0]
    res[1] = this[offset+1] xor mask[1]
    res[2] = this[offset+2] xor mask[2]
    res[3] = this[offset+3] xor mask[3]
    return res
}

/** XOR4 in place */
inline fun ByteArray.xor4ip(offset: Int, mask: ByteArray) {
    this[offset+0] = this[offset+0] xor mask[0]
    this[offset+1] = this[offset+1] xor mask[1]
    this[offset+2] = this[offset+2] xor mask[2]
    this[offset+3] = this[offset+3] xor mask[3]
}

inline fun ByteArray.mapBytes(m: (Int, Byte) -> Byte) =
    ByteArray(size) { i ->
        m(i, this[i])
    }
