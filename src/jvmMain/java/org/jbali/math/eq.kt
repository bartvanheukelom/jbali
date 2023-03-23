@file:Suppress("NOTHING_TO_INLINE")

package org.jbali.math

// ----------- universal equality -------------- //

// unfortunately can't use equals i.e. ==, because it's a member in Any

inline infix fun ULong.eq(b: ULong) = this == b
inline infix fun ULong.eq(b: Long) = b >= 0 && this == b.toULong()
inline infix fun ULong.eq(b: Int) = b >= 0 && this == b.toULong()
inline infix fun ULong.eq(b: Short) = b >= 0 && this == b.toULong()
inline infix fun ULong.eq(b: Byte) = b >= 0 && this == b.toULong()
inline infix fun Long.eq(b: ULong) = b eq this
inline infix fun Int.eq(b: ULong) = b eq this
inline infix fun Short.eq(b: ULong) = b eq this
inline infix fun Byte.eq(b: ULong) = b eq this

inline infix fun UInt.eq(b: UInt) = this == b
inline infix fun UInt.eq(b: Long) = b >= 0 && this == b.toUInt()
inline infix fun UInt.eq(b: Int) = b >= 0 && this == b.toUInt()
inline infix fun UInt.eq(b: Short) = b >= 0 && this == b.toUInt()
inline infix fun UInt.eq(b: Byte) = b >= 0 && this == b.toUInt()
inline infix fun Long.eq(b: UInt) = b eq this
inline infix fun Int.eq(b: UInt) = b eq this
inline infix fun Short.eq(b: UInt) = b eq this
inline infix fun Byte.eq(b: UInt) = b eq this

inline infix fun UShort.eq(b: UShort) = this == b
inline infix fun UShort.eq(b: Long) = b >= 0 && this == b.toUShort()
inline infix fun UShort.eq(b: Int) = b >= 0 && this == b.toUShort()
inline infix fun UShort.eq(b: Short) = b >= 0 && this == b.toUShort()
inline infix fun UShort.eq(b: Byte) = b >= 0 && this == b.toUShort()
inline infix fun Long.eq(b: UShort) = b eq this
inline infix fun Int.eq(b: UShort) = b eq this
inline infix fun Short.eq(b: UShort) = b eq this
inline infix fun Byte.eq(b: UShort) = b eq this

inline infix fun UByte.eq(b: UByte) = this == b
inline infix fun UByte.eq(b: Long) = b >= 0 && this == b.toUByte()
inline infix fun UByte.eq(b: Int) = b >= 0 && this == b.toUByte()
inline infix fun UByte.eq(b: Short) = b >= 0 && this == b.toUByte()
inline infix fun UByte.eq(b: Byte) = b >= 0 && this == b.toUByte()
inline infix fun Long.eq(b: UByte) = b eq this
inline infix fun Int.eq(b: UByte) = b eq this
inline infix fun Short.eq(b: UByte) = b eq this
inline infix fun Byte.eq(b: UByte) = b eq this

inline infix fun Long.eq(b: Long) = this == b
inline infix fun Long.eq(b: Int) = this == b.toLong()
inline infix fun Long.eq(b: Short) = this == b.toLong()
inline infix fun Long.eq(b: Byte) = this == b.toLong()
inline infix fun Int.eq(b: Long) = b eq this
inline infix fun Short.eq(b: Long) = b eq this
inline infix fun Byte.eq(b: Long) = b eq this

inline infix fun Int.eq(b: Int) = this == b
inline infix fun Int.eq(b: Short) = this == b.toInt()
inline infix fun Int.eq(b: Byte) = this == b.toInt()
inline infix fun Short.eq(b: Int) = b eq this
inline infix fun Byte.eq(b: Int) = b eq this

inline infix fun Short.eq(b: Short) = this == b
inline infix fun Short.eq(b: Byte) = this == b.toShort()
inline infix fun Byte.eq(b: Short) = b eq this

inline infix fun Byte.eq(b: Byte) = this == b
