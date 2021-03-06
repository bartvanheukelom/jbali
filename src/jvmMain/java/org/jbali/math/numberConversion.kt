package org.jbali.math

import org.jbali.reflect.kClass
import java.math.BigDecimal
import java.math.BigInteger

fun Long.toIntExact(): Int =
        Math.toIntExact(this)

fun Double.toLongExact(): Long =
        toLong().also { l ->
            if (l.toDouble() != this) {
                throw ArithmeticException("$this is not representable as long")
            }
        }

/**
 * Convert this number to a [BigDecimal] without precision loss.
 * Only works for the standard subclasses of [Number]:
 * - [BigDecimal] and [BigInteger]
 * - [Float] and [Double]
 * - [Byte], [Short], [Int] and [Long] (NOT [Char])
 *
 * @throws ArithmeticException if this number is not one of the standard subclasses.
 */
fun Number.toBigDecimal(): BigDecimal =
        when (this) {

            is BigDecimal -> this
            is BigInteger -> BigDecimal(this)

            is Float -> BigDecimal(this.toDouble())
            is Double -> BigDecimal(this)

            is Int -> BigDecimal(this)
            is Long -> BigDecimal(this)
            is Byte, is Short -> BigDecimal(this.toInt())

            else -> throw ArithmeticException("$this is not representable as BigDecimal")
        }

/**
 * Convert this [BigDecimal] to a double without precision loss.
 * @throws ArithmeticException if @OptIn(ExperimentalSerializationApi::class)precision would be lost.
 */
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") // kotlin.Number does not declare doubleValue
fun BigDecimal.toDoubleExact(): Double =
        (this as java.lang.Number).doubleValue().also { d ->
            val bd = BigDecimal(d)
            // (12.0).compareTo(12) returns 0, equals returns false
            if (bd.compareTo(this) != 0) {
                throw ArithmeticException("BigDecimal($this) is not representable as double (conversion yields $d -> BigDecimal($bd))")
            }
        }

/**
 * Convert this number to a double without precision loss.
 * @throws ArithmeticException if precision would be lost.
 */
fun Number.toDoubleExact(): Double =
        when (this) {
            is Double -> this
            is Byte, is Short, is Int -> this.toDouble()

            else -> this.toBigDecimal().toDoubleExact()
        }

/**
 * Convert this number to a long without precision loss.
 * @throws ArithmeticException if precision would be lost.
 */
fun Number.toLongExact(): Long =
        when (this) {
            is Long -> this
            is Byte, is Short, is Int -> this.toLong()
            is BigDecimal -> this.toLongExact()
            is BigInteger -> this.toLongExact()

            is Float -> this.toDouble().toLongExact()
            is Double -> this.toLongExact()

            else -> throw ArithmeticException("Cannot do ($this as ${this.kClass.qualifiedName}).toLongExact()")
        }
