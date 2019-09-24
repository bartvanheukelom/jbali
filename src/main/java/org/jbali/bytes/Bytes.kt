package org.jbali.bytes

import java.io.DataOutput
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.xor


infix fun ByteArray.xor(mask: ByteArray): ByteArray {
    return if (size == 4) {
        check(mask.size >= 4)
        (Bytes4(this) xor Bytes4(mask)).array
    } else {
        check(mask.size > size)
        mapBytes { i, b -> b xor mask[i] }
    }
}

// TODO make the value an Int
inline class Bytes4(val array: ByteArray = ByteArray(4)) {
    inline operator fun get(i: Int): Byte = array[i]
    inline operator fun set(i: Int, value: Byte) { array[i] = value }
}

/**
 * Return this array wrapped in Bytes4 if its size == 4,
 * otherwise throws IllegalArgumentException.
 */
fun ByteArray.checkedBytes4(): Bytes4 =
        if (size == 4) Bytes4(this)
        else throw IllegalArgumentException("Receiver size $size != 4")

fun DataOutput.write(b4: Bytes4) = write(b4.array)

/**
 * XOR this byte array with a mask.
 */
@Suppress("NOTHING_TO_INLINE")
inline infix fun Bytes4.xor(mask: Bytes4): Bytes4 {
    val res = Bytes4()
    repeat4 {
        res[it] = this[it] xor mask[it]
    }
    return res
}

/**
 * XOR 4 bytes from this byte array, starting at offset, with a mask of 4 bytes long, into a new array.
 * Does not check whether the given parameters are valid!
 */
@Suppress("NOTHING_TO_INLINE")
inline fun ByteArray.xor4(offset: Int, mask: Bytes4): Bytes4 {
    val res = Bytes4()
    repeat4 {
        res[it] = this[offset+it] xor mask[it]
    }
    return res
}

/** XOR4 in place */
@Suppress("NOTHING_TO_INLINE")
inline fun ByteArray.xor4ip(offset: Int, mask: Bytes4) {
    repeat4 {
        this[offset+it] = this[offset+it] xor mask[it]
    }
}

inline fun ByteArray.mapBytes(m: (Int, Byte) -> Byte) =
    ByteArray(size) { i ->
        m(i, this[i])
    }

inline fun repeat4(block: (i: Int) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    block(0)
    block(1)
    block(2)
    block(3)
}
