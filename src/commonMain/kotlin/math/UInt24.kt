@file:Suppress("NOTHING_TO_INLINE")
package org.jbali.math

import kotlin.experimental.*
import kotlin.jvm.*

@JvmInline
value class UInt24 @PublishedApi internal constructor(@PublishedApi internal val data: Int) : Comparable<UInt24> {
    
    companion object {
    
        @PublishedApi
        internal const val MASK = 0xFF_FFFF
        
        /**
         * A constant holding the minimum value an instance of UInt24 can have, `0`.
         */
        val MIN_VALUE: UInt24 = UInt24(0)
        
        /**
         * A constant holding the maximum value an instance of UInt24 can have:
         * - `2^24 -1`
         * - i.e. `0xFF_FFFF`
         * - i.e. `16777215`
         */
        val MAX_VALUE: UInt24 = UInt24(MASK)
        
        /**
         * The number of bytes used to represent an instance of UInt24 in a binary form, i.e. `3`.
         */
        const val SIZE_BYTES: Int = 3
        
        /**
         * The number of bits used to represent an instance of UInt24 in a binary form, i.e. `24`.
         */
        const val SIZE_BITS: Int = 24
        
    }
    
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    inline operator fun compareTo(other: UByte): Int =
        toUInt().compareTo(other.toUInt())
    
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    inline operator fun compareTo(other: UShort): Int =
        toUInt().compareTo(other.toUInt())
    
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    @Suppress("OVERRIDE_BY_INLINE")
    override inline operator fun compareTo(other: UInt24): Int =
        toUInt().compareTo(other.toUInt())
    
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    inline operator fun compareTo(other: UInt): Int = this.toULong().compareTo(other)
    
    /**
     * Compares this value with the specified value for order.
     * Returns zero if this value is equal to the specified other value, a negative number if it's less than other,
     * or a positive number if it's greater than other.
     */
    inline operator fun compareTo(other: ULong): Int = this.toULong().compareTo(other)
    
    /** Adds the other value to this value. */
    inline operator fun plus(other: UByte): UInt24 = this.plus(other.toUInt()).toUInt24()
    /** Adds the other value to this value. */
    inline operator fun plus(other: UShort): UInt24 = this.plus(other.toUInt()).toUInt24()
    /** Adds the other value to this value. */
    inline operator fun plus(other: UInt24): UInt24 = this.data.plus(other.data).toUInt24()
    /** Adds the other value to this value. */
    inline operator fun plus(other: UInt): UInt = toUInt().plus(other)
    /** Adds the other value to this value. */
    inline operator fun plus(other: ULong): ULong = this.toULong().plus(other)
    
    /** Subtracts the other value from this value. */
    inline operator fun minus(other: UByte): UInt24 = toUInt().minus(other).toUInt24()
    /** Subtracts the other value from this value. */
    inline operator fun minus(other: UShort): UInt24 = toUInt().minus(other).toUInt24()
    /** Subtracts the other value from this value. */
    inline operator fun minus(other: UInt24): UInt24 = this.data.minus(other.data).toUInt24()
    /** Subtracts the other value from this value. */
    inline operator fun minus(other: UInt): UInt = toUInt().minus(other)
    /** Subtracts the other value from this value. */
    inline operator fun minus(other: ULong): ULong = this.toULong().minus(other)
    
    /** Multiplies this value by the other value. */
    inline operator fun times(other: UByte): UInt = this.times(other.toUInt())
    /** Multiplies this value by the other value. */
    inline operator fun times(other: UShort): UInt = this.times(other.toUInt())
    /** Multiplies this value by the other value. */
    inline operator fun times(other: UInt24): UInt24 = toUInt().times(other.toUInt()).toUInt24()
    /** Multiplies this value by the other value. */
    inline operator fun times(other: UInt): UInt = toUInt().times(other)
    /** Multiplies this value by the other value. */
    inline operator fun times(other: ULong): ULong = this.toULong().times(other)
    
    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    inline operator fun div(other: UByte): UInt = this.div(other.toUInt())
    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    inline operator fun div(other: UShort): UInt = this.div(other.toUInt())
    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    inline operator fun div(other: UInt24): UInt24 = this.toUInt().div(other.toUInt()).toUInt24()
    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    inline operator fun div(other: UInt): UInt = this.toUInt().div(other)
    /** Divides this value by the other value, truncating the result to an integer that is closer to zero. */
    inline operator fun div(other: ULong): ULong = this.toULong().div(other)
    
    /**
     * Calculates the remainder of truncating division of this value by the other value.
     *
     * The result is always less than the divisor.
     */
    inline operator fun rem(other: UByte): UInt24 = toUInt().rem(other.toUInt()).toUInt24()
    /**
     * Calculates the remainder of truncating division of this value by the other value.
     *
     * The result is always less than the divisor.
     */
    inline operator fun rem(other: UShort): UInt24 = toUInt().rem(other.toUInt()).toUInt24()
    /**
     * Calculates the remainder of truncating division of this value by the other value.
     *
     * The result is always less than the divisor.
     */
    inline operator fun rem(other: UInt24): UInt24 = toUInt().rem(other.toUInt()).toUInt24()
    /**
     * Calculates the remainder of truncating division of this value by the other value.
     *
     * The result is always less than the divisor.
     */
    inline operator fun rem(other: ULong): ULong = this.toULong().rem(other)
    
    /**
     * Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    inline fun floorDiv(other: UByte): UInt = this.floorDiv(other.toUInt())
    /**
     * Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    inline fun floorDiv(other: UShort): UInt = this.floorDiv(other.toUInt())
    /**
     * Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    inline fun floorDiv(other: UInt): UInt = div(other)
    /**
     * Divides this value by the other value, flooring the result to an integer that is closer to negative infinity.
     *
     * For unsigned types, the results of flooring division and truncating division are the same.
     */
    inline fun floorDiv(other: ULong): ULong = this.toULong().floorDiv(other)
    
    /**
     * Calculates the remainder of flooring division of this value by the other value.
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    inline fun mod(other: UByte): UByte = this.mod(other.toUInt()).toUByte()
    /**
     * Calculates the remainder of flooring division of this value by the other value.
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    inline fun mod(other: UShort): UShort = this.mod(other.toUInt()).toUShort()
    /**
     * Calculates the remainder of flooring division of this value by the other value.
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    inline fun mod(other: UInt24): UInt24 = rem(other)
    /**
     * Calculates the remainder of flooring division of this value by the other value.
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    inline fun mod(other: UInt): UInt = this.toUInt().mod(other)
    /**
     * Calculates the remainder of flooring division of this value by the other value.
     *
     * The result is always less than the divisor.
     *
     * For unsigned types, the remainders of flooring division and truncating division are the same.
     */
    inline fun mod(other: ULong): ULong = this.toULong().mod(other)
    
    /**
     * Returns this value incremented by one.
     */
    inline operator fun inc(): UInt24 = UInt24(data.inc())
    
    /**
     * Returns this value decremented by one.
     */
    inline operator fun dec(): UInt24 = UInt24(data.dec())
    
    /** Creates a range from this value to the specified [other] value. */
//    inline operator fun rangeTo(other: UInt24): UIntRange = UInt24Range(this, other)
    
    /**
     * Shifts this value left by the [bitCount] number of bits.
     *
     * Note that only the five lowest-order bits of the [bitCount] are used as the shift distance.
     * The shift distance actually used is therefore always in the range `0..31`.
     */
    inline infix fun shl(bitCount: Int): UInt24 = (data shl bitCount).toUInt24()
    
    /**
     * Shifts this value right by the [bitCount] number of bits, filling the leftmost bits with zeros.
     *
     * Note that only the five lowest-order bits of the [bitCount] are used as the shift distance.
     * The shift distance actually used is therefore always in the range `0..31`.
     */
    inline infix fun shr(bitCount: Int): UInt24 = UInt24(data ushr bitCount)
    
    /** Performs a bitwise AND operation between the two values. */
    inline infix fun and(other: UInt24): UInt24 = UInt24(this.data and other.data)
    /** Performs a bitwise OR operation between the two values. */
    inline infix fun or(other: UInt24): UInt24 = UInt24(this.data or other.data)
    /** Performs a bitwise XOR operation between the two values. */
    inline infix fun xor(other: UInt24): UInt24 = UInt24(this.data xor other.data)
    /** Inverts the bits in this value. */
    inline fun inv(): UInt24 = UInt24(data.inv())
    
    /**
     * Converts this [UInt24] value to [Byte].
     *
     * If this value is less than or equals to [Byte.MAX_VALUE], the resulting `Byte` value represents
     * the same numerical value as this `UInt`.
     *
     * The resulting `Byte` value is represented by the least significant 8 bits of this `UInt` value.
     * Note that the resulting `Byte` value may be negative.
     */
    inline fun toByte(): Byte = data.toByte()
    /**
     * Converts this [UInt24] value to [Short].
     *
     * If this value is less than or equals to [Short.MAX_VALUE], the resulting `Short` value represents
     * the same numerical value as this `UInt`.
     *
     * The resulting `Short` value is represented by the least significant 16 bits of this `UInt` value.
     * Note that the resulting `Short` value may be negative.
     */
    inline fun toShort(): Short = data.toShort()
    /**
     * Converts this [UInt24] value to [Int].
     *
     * The resulting `Int` value represents the same numerical value as this `UInt24`.
     *
     * The least significant 24 bits of the resulting `Int` value are the same as the bits of this `UInt24` value,
     * whereas the most significant 8 bits are filled with zeros.
     */
    inline fun toInt(): Int = data and MASK
    /**
     * Converts this [UInt24] value to [Long].
     *
     * The resulting `Long` value represents the same numerical value as this `UInt`.
     *
     * The least significant 24 bits of the resulting `Long` value are the same as the bits of this `UInt24` value,
     * whereas the most significant 40 bits are filled with zeros.
     */
    inline fun toLong(): Long = data.toLong() and MASK.toLong()
    
    /**
     * Converts this [UInt24] value to [UByte].
     *
     * If this value is less than or equals to [UByte.MAX_VALUE], the resulting `UByte` value represents
     * the same numerical value as this `UInt24`.
     *
     * The resulting `UByte` value is represented by the least significant 8 bits of this `UInt` value.
     */
    inline fun toUByte(): UByte = data.toUByte()
    /**
     * Converts this [UInt24] value to [UShort].
     *
     * If this value is less than or equals to [UShort.MAX_VALUE], the resulting `UShort` value represents
     * the same numerical value as this `UInt24`.
     *
     * The resulting `UShort` value is represented by the least significant 16 bits of this `UInt` value.
     */
    inline fun toUShort(): UShort = data.toUShort()
    /**
     * Converts this [UInt24] value to [UInt].
     *
     * The resulting `UInt` value represents the same numerical value as this `UInt24`.
     *
     * The least significant 24 bits of the resulting `UInt` value are the same as the bits of this `UInt24` value,
     * whereas the most significant 8 bits are filled with zeros.
     */
    inline fun toUInt(): UInt = data.toUInt()
    /**
     * Converts this [UInt24] value to [ULong].
     *
     * The resulting `ULong` value represents the same numerical value as this `UInt`.
     *
     * The least significant 24 bits of the resulting `ULong` value are the same as the bits of this `UInt` value,
     * whereas the most significant 40 bits are filled with zeros.
     */
    inline fun toULong(): ULong = toUInt().toULong()
    
    /**
     * Converts this [UInt24] value to [Float].
     *
     * The resulting value is the closest `Float` to this `UInt` value.
     * In case when this `UInt` value is exactly between two `Float`s,
     * the one with zero at least significant bit of mantissa is selected.
     */
    inline fun toFloat(): Float = this.toDouble().toFloat()
    /**
     * Converts this [UInt24] value to [Double].
     *
     * The resulting `Double` value represents the same numerical value as this `UInt`.
     */
    inline fun toDouble(): Double = toUInt().toDouble()
    
    override fun toString(): String = toLong().toString()
    
}

/**
 * Converts this [Byte] value to [UInt].
 *
 * If this value is positive, the resulting `UInt` value represents the same numerical value as this `Byte`.
 *
 * The least significant 8 bits of the resulting `UInt` value are the same as the bits of this `Byte` value,
 * whereas the most significant 24 bits are filled with the sign bit of this value.
 */
inline fun Byte.toUInt24(): UInt24 = toUInt().toUInt24()
/**
 * Converts this [Short] value to [UInt].
 *
 * If this value is positive, the resulting `UInt` value represents the same numerical value as this `Short`.
 *
 * The least significant 16 bits of the resulting `UInt` value are the same as the bits of this `Short` value,
 * whereas the most significant 16 bits are filled with the sign bit of this value.
 */
inline fun Short.toUInt24(): UInt24 = toUInt().toUInt24()
/**
 * Converts this [Int] value to [UInt].
 *
 * If this value is positive, the resulting `UInt` value represents the same numerical value as this `Int`.
 *
 * The resulting `UInt` value has the same binary representation as this `Int` value.
 */
inline fun Int.toUInt24(): UInt24 = toUInt().toUInt24()
/**
 * Converts this [Long] value to [UInt].
 *
 * If this value is positive and less than or equals to [UInt.MAX_VALUE], the resulting `UInt` value represents
 * the same numerical value as this `Long`.
 *
 * The resulting `UInt` value is represented by the least significant 32 bits of this `Long` value.
 */
inline fun Long.toUInt24(): UInt24 = toUInt().toUInt24()

// all the converters / constructors must go through here
inline fun UInt.toUInt24(): UInt24 = UInt24(this.toInt() and UInt24.MASK)

/**
 * Converts this [Float] value to [UInt].
 *
 * The fractional part, if any, is rounded down towards zero.
 * Returns zero if this `Float` value is negative or `NaN`, [UInt.MAX_VALUE] if it's bigger than `UInt.MAX_VALUE`.
 */
inline fun Float.toUInt24(): UInt24 = toUInt().toUInt24()
/**
 * Converts this [Double] value to [UInt].
 *
 * The fractional part, if any, is rounded down towards zero.
 * Returns zero if this `Double` value is negative or `NaN`, [UInt.MAX_VALUE] if it's bigger than `UInt.MAX_VALUE`.
 */
inline fun Double.toUInt24(): UInt24 = toUInt().toUInt24()
