package org.jbali.bytes

import java.io.DataOutput

/**
 * Compile time wrapper around a byte array of length 4.
 * WARNING! the primary constructor does no bounds checking on the array,
 * and this class uses Unsafe code. if the array is <= 4, this may result
 * in a JVM crash.
 */
@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class Bytes4(val array: ByteArray = ByteArray(4)) {
    
    inline operator fun get(i: Int): Byte = theUnsafe.getByte(array, byteArrOffset + i.toLong())
    inline operator fun set(i: Int, value: Byte) { theUnsafe.putByte(array, byteArrOffset + i.toLong(), value) }

    /** May use native byte order instead of JVM's default Big Endian! */
    constructor(v: Int) : this(ByteArray(4).also { theUnsafe.putInt(it, byteArrOffset, v) })

    /** May use native byte order instead of JVM's default Big Endian! */
    inline fun getInt() = theUnsafe.getInt(array, byteArrOffset)

    inline infix fun xor(rhs: Bytes4) = Bytes4(getInt() xor rhs.getInt())

    /**
     * XOR 4 bytes from this byte array, starting at offset, with a mask of 4 bytes long, into a new array.
     * Does not check whether the given parameters are valid!
     */
    inline fun xor(rhs: ByteArray, offset: Int): Bytes4 {
        return Bytes4(getInt() xor theUnsafe.getInt(rhs, byteArrOffset + offset.toLong()))
    }
    
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
 * XOR 4 bytes from this byte array, starting at offset, with a mask of 4 bytes long, into a new array.
 * Does not check whether the given parameters are valid!
 */
@Suppress("NOTHING_TO_INLINE")
inline fun ByteArray.xor(offset: Int, mask: Bytes4) = mask.xor(this, offset)

/** XOR4 in place */
@Suppress("NOTHING_TO_INLINE")
inline fun ByteArray.xor4ip(offset: Int, mask: Bytes4) {
    theUnsafe.putInt(this, byteArrOffset + offset.toLong(), theUnsafe.getInt(this, byteArrOffset + offset.toLong()) xor mask.getInt())
}
