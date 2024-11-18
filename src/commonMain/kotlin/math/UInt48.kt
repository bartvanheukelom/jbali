@file:Suppress("NOTHING_TO_INLINE")
package org.jbali.math

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * The UInt48 class represents a 48-bit unsigned integer value and provides the standard operations on it.
 *
 * The value is stored as a [Long] internally, but this is an implementation detail that is subject to change.
 */
@Serializable
@JvmInline
value class UInt48 @PublishedApi internal constructor(@PublishedApi internal val data: Long) : Comparable<UInt48> {
    
    companion object {
    
        @PublishedApi
        internal const val MASK = 0xFFFF_FFFF_FFFF
        
        /**
         * A constant holding the minimum value an instance of UInt48 can have, `0`.
         */
        val MIN_VALUE: UInt48 = UInt48(0L)
        
        /**
         * A constant holding the maximum value an instance of UInt48 can have:
         * - `2^48 -1`
         * - i.e. `0xFFFF_FFFF_FFFF`
         * - i.e. `281474976710655`
         */
        val MAX_VALUE: UInt48 = UInt48(MASK)
        
        /**
         * The number of bytes used to represent an instance of UInt48 in a binary form, i.e. `6`.
         */
        const val SIZE_BYTES: Int = 6
        
        /**
         * The number of bits used to represent an instance of UInt48 in a binary form, i.e. `48`.
         */
        const val SIZE_BITS: Int = 48
        
    }
    
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @Suppress("OVERRIDE_BY_INLINE")
    override inline operator fun compareTo(other: UInt48): Int =
        toLong().compareTo(other.toLong())
    
    /** Adds the other value to this value. */
    inline operator fun plus(other: UInt48): UInt48 = this.data.plus(other.data).toUInt48()
    /** Subtracts the other value from this value. */
    inline operator fun minus(other: UInt48): UInt48 = this.data.minus(other.data).toUInt48()
    /** Multiplies this value by the other value. */
    inline operator fun times(other: UInt48): UInt48 = this.data.times(other.data).toUInt48()
    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    inline operator fun div(other: UInt48): UInt48 = this.data.div(other.data).toUInt48()
    /** Calculates the remainder of truncating division of this value by the other value. */
    inline operator fun rem(other: UInt48): UInt48 = this.data.rem(other.data).toUInt48()
    
    /**
     * Returns this value incremented by one.
     */
    inline operator fun inc(): UInt48 = UInt48(data.inc())
    
    /**
     * Returns this value decremented by one.
     */
    inline operator fun dec(): UInt48 = UInt48(data.dec())
    
    /** Performs a bitwise AND operation between the two values. */
    inline infix fun and(other: UInt48): UInt48 = UInt48(this.data and other.data)
    /** Performs a bitwise OR operation between the two values. */
    inline infix fun or(other: UInt48): UInt48 = UInt48(this.data or other.data)
    /** Performs a bitwise XOR operation between the two values. */
    inline infix fun xor(other: UInt48): UInt48 = UInt48(this.data xor other.data)
    /** Inverts the bits in this value. */
    inline fun inv(): UInt48 = UInt48(data.inv())
    
    /**
     * Shifts this value left by the [bitCount] number of bits.
     *
     * Note that only the six lowest-order bits of the [bitCount] are used as the shift distance.
     * The shift distance actually used is therefore always in the range `0..63`.
     */
    inline infix fun shl(bitCount: Int): UInt48 = (data shl bitCount).toUInt48()
    
    /**
     * Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with zeros.
     *
     * Note that only the six lowest-order bits of the [bitCount] are used as the shift distance.
     * The shift distance actually used is therefore always in the range `0..63`.
     */
    inline infix fun shr(bitCount: Int): UInt48 = UInt48(data ushr bitCount)
    
    /**
     * Converts this [UInt48] value to [Byte].
     *
     * If this value is less than or equals to [Byte.MAX_VALUE], the resulting `Byte` value represents
     * the same numerical value as this `UInt48`.
     *
     * The resulting `Byte` value is represented by the least significant 8 bits of this `UInt48` value.
     * Note that the resulting `Byte` value may be negative.
     */
    inline fun toByte(): Byte = data.toByte()
    /**
     * Converts this [UInt48] value to [Short].
     *
     * If this value is less than or equals to [Short.MAX_VALUE], the resulting `Short` value represents
     * the same numerical value as this `UInt48`.
     *
     * The resulting `Short` value is represented by the least significant 16 bits of this `UInt48` value.
     * Note that the resulting `Short` value may be negative.
     */
    inline fun toShort(): Short = data.toShort()
    /**
     * Converts this [UInt48] value to [Int].
     *
     * The resulting `Int` value represents the same numerical value as this `UInt48`.
     *
     * The least significant 32 bits of the resulting `Int` value are the same as the bits of this `UInt48` value,
     * whereas the most significant 16 bits are filled with zeros.
     */
    inline fun toInt(): Int = data.toInt()
    /**
     * Converts this [UInt48] value to [Long].
     *
     * The resulting `Long` value represents the same numerical value as this `UInt48`.
     *
     * The least significant 48 bits of the resulting `Long` value are the same as the bits of this `UInt48` value,
     * whereas the most significant 16 bits are filled with zeros.
     */
    inline fun toLong(): Long = data
    
    /**
     * Converts this [UInt48] value to [UByte].
     *
     * If this value is less than or equals to [UByte.MAX_VALUE], the resulting `UByte` value represents
     * the same numerical value as this `UInt48`.
     *
     * The resulting `UByte` value is represented by the least significant 8 bits of this `UInt48` value.
     */
    inline fun toUByte(): UByte = data.toUByte()
    /**
     * Converts this [UInt48] value to [UShort].
     *
     * If this value is less than or equals to [UShort.MAX_VALUE], the resulting `UShort` value represents
     * the same numerical value as this `UInt48`.
     *
     * The resulting `UShort` value is represented by the least significant 16 bits of this `UInt48` value.
     */
    inline fun toUShort(): UShort = data.toUShort()
    /**
     * Converts this [UInt48] value to [UInt].
     *
     * If this value is less than or equals to [UInt.MAX_VALUE], the resulting `UInt` value represents
     * the same numerical value as this `UInt48`.
     *
     * The resulting `UInt` value is represented by the least significant 32 bits of this `UInt48` value.
     */
    inline fun toUInt(): UInt = data.toUInt()
    /**
     * Converts this [UInt48] value to [ULong].
     *
     * The resulting `ULong` value represents the same numerical value as this `UInt48`.
     *
     * The least significant 48 bits of the resulting `ULong` value are the same as the bits of this `UInt48` value,
     * whereas the most significant 16 bits are filled with zeros.
     */
    inline fun toULong(): ULong = data.toULong()
    
    /**
     * Converts this [UInt48] value to [Float].
     *
     * The resulting value is the closest `Float` to this `UInt48` value.
     * In case when this `UInt48` value is exactly between two `Float`s,
     * the one with zero at least significant bit of mantissa is selected.
     */
    inline fun toFloat(): Float = toDouble().toFloat()
    /**
     * Converts this [UInt48] value to [Double].
     *
     * The resulting `Double` value represents the same numerical value as this `UInt48`.
     */
    inline fun toDouble(): Double = data.toDouble()
    
    override fun toString(): String = data.toString()
    
}

/**
 * Converts this [Byte] value to [UInt48].
 *
 * If this value is positive, the resulting `UInt48` value represents the same numerical value as this `Byte`.
 *
 * The least significant 8 bits of the resulting `UInt48` value are the same as the bits of this `Byte` value,
 * whereas the most significant 40 bits are filled with the sign bit of this value.
 */
inline fun Byte.toUInt48(): UInt48 = toLong().toUInt48()
/**
 * Converts this [Short] value to [UInt48].
 *
 * If this value is positive, the resulting `UInt48` value represents the same numerical value as this `Short`.
 *
 * The least significant 16 bits of the resulting `UInt48` value are the same as the bits of this `Short` value,
 * whereas the most significant 32 bits are filled with the sign bit of this value.
 */
inline fun Short.toUInt48(): UInt48 = toLong().toUInt48()
/**
 * Converts this [Int] value to [UInt48].
 *
 * If this value is positive, the resulting `UInt48` value represents the same numerical value as this `Int`.
 *
 * The least significant 32 bits of the resulting `UInt48` value are the same as the bits of this `Int` value,
 * whereas the most significant 16 bits are filled with the sign bit of this value.
 */
inline fun Int.toUInt48(): UInt48 = toLong().toUInt48()
/**
 * Converts this [Long] value to [UInt48].
 *
 * If this value is positive and less than or equals to [ULong.MAX_VALUE], the resulting `UInt48` value represents
 * the same numerical value as this `Long`.
 *
 * The resulting `UInt48` value is represented by the least significant 48 bits of this `Long` value.
 */
inline fun Long.toUInt48(): UInt48 = UInt48(this and UInt48.MASK)

// all the converters / constructors must go through here
inline fun ULong.toUInt48(): UInt48 = toLong().toUInt48()

/**
 * Converts this [Float] value to [UInt48].
 *
 * The fractional part, if any, is rounded down towards zero.
 * Returns zero if this `Float` value is negative or `NaN`, [UInt48.MAX_VALUE] if it's bigger than `UInt48.MAX_VALUE`.
 */
inline fun Float.toUInt48(): UInt48 = toLong().toUInt48()
/**
 * Converts this [Double] value to [UInt48].
 *
 * The fractional part, if any, is rounded down towards zero.
 * Returns zero if this `Double` value is negative or `NaN`, [UInt48.MAX_VALUE] if it's bigger than `UInt48.MAX_VALUE`.
 */
inline fun Double.toUInt48(): UInt48 = toLong().toUInt48()
