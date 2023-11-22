package org.jbali.bytes

import java.nio.ByteBuffer



/**
 * Return the contents of this ByteArray as a *big-endian* 64-bit signed integer, i.e. a Long.
 * @throws IllegalArgumentException if the ByteArray is not exactly 8 bytes long.
 */
fun ByteArray.asLong(): Long {
    require(size == 8) { "ByteArray must be exactly 8 bytes for conversion to Long (int64)" }
    return ByteBuffer.wrap(this).long
}

/**
 * Return the contents of this ByteArray as a *big-endian* 32-bit signed integer, i.e. an Int.
 * @throws IllegalArgumentException if the ByteArray is not exactly 4 bytes long.
 */
fun ByteArray.asInt(): Int {
    require(size == 4) { "ByteArray must be exactly 4 bytes for conversion to Int (int32)" }
    return ByteBuffer.wrap(this).int
}

/**
 * Return the contents of this ByteArray as a *big-endian* 16-bit signed integer, i.e. a Short.
 * @throws IllegalArgumentException if the ByteArray is not exactly 2 bytes long.
 */
fun ByteArray.asShort(): Short {
    require(size == 2) { "ByteArray must be exactly 2 bytes for conversion to Short (int16)" }
    return ByteBuffer.wrap(this).short
}

/**
 * Return the contents of this ByteArray as an 8-bit signed integer, i.e. a Byte.
 * @throws IllegalArgumentException if the ByteArray is not exactly 1 byte long.
 */
fun ByteArray.asByte(): Byte {
    require(size == 1) { "ByteArray must be exactly 1 byte for conversion to Byte (int8)" }
    return this[0]
}


/**
 * Return the contents of this ByteArray as a *big-endian* 64-bit unsigned integer, i.e. a ULong.
 * @throws IllegalArgumentException if the ByteArray is not exactly 8 bytes long.
 */
fun ByteArray.asULong() = asLong().toULong()

/**
 * Return the contents of this ByteArray as a *big-endian* 32-bit unsigned integer, i.e. a UInt.
 * @throws IllegalArgumentException if the ByteArray is not exactly 4 bytes long.
 */
fun ByteArray.asUInt() = asInt().toUInt()

/**
 * Return the contents of this ByteArray as a *big-endian* 16-bit unsigned integer, i.e. a UShort.
 * @throws IllegalArgumentException if the ByteArray is not exactly 2 bytes long.
 */
fun ByteArray.asUShort() = asShort().toUShort()

/**
 * Return the contents of this ByteArray as an 8-bit unsigned integer, i.e. a UByte.
 * @throws IllegalArgumentException if the ByteArray is not exactly 1 byte long.
 */
fun ByteArray.asUByte() = asByte().toUByte()


/**
 * Return the contents of this ByteArray as a *big-endian* 64-bit floating point number, i.e. a Double.
 * @throws IllegalArgumentException if the ByteArray is not exactly 8 bytes long.
 */
fun ByteArray.asDouble(): Double {
    require(size == 8) { "ByteArray must be exactly 8 bytes for conversion to Double (float64)" }
    return ByteBuffer.wrap(this).double
}

/**
 * Return the contents of this ByteArray as a *big-endian* 32-bit floating point number, i.e. a Float.
 * @throws IllegalArgumentException if the ByteArray is not exactly 4 bytes long.
 */
fun ByteArray.asFloat(): Float {
    require(size == 4) { "ByteArray must be exactly 4 bytes for conversion to Float (float32)" }
    return ByteBuffer.wrap(this).float
}


/**
 * Return the contents of this ByteArray as a 64-bit signed integer, i.e. a Long,
 * *using the native byte order of the JVM*!
 * @throws IllegalArgumentException if the ByteArray is not exactly 8 bytes long.
 */
fun ByteArray.asNativeLong(): Long {
    require(size == 8) { "ByteArray must be exactly 8 bytes for conversion to Long (int64)" }
    return theUnsafe.getLong(this, byteArrOffset)
}

/**
 * Return the contents of this ByteArray as a 32-bit signed integer, i.e. an Int,
 * *using the native byte order of the JVM*!
 * @throws IllegalArgumentException if the ByteArray is not exactly 4 bytes long.
 */
fun ByteArray.asNativeInt(): Int {
    require(size == 4) { "ByteArray must be exactly 4 bytes for conversion to Int (int32)" }
    return theUnsafe.getInt(this, byteArrOffset)
}

/**
 * Return the contents of this ByteArray as a 16-bit signed integer, i.e. a Short,
 * *using the native byte order of the JVM*!
 * @throws IllegalArgumentException if the ByteArray is not exactly 2 bytes long.
 */
fun ByteArray.asNativeShort(): Short {
    require(size == 2) { "ByteArray must be exactly 2 bytes for conversion to Short (int16)" }
    return theUnsafe.getShort(this, byteArrOffset)
}

// no asNativeByte() because that's the same as asByte()
