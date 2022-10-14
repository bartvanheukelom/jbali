package org.jbali.math

import kotlinx.serialization.Serializable

infix fun Int.ceilDiv(rhs: Int): Int {
    val div = this / rhs
    return if (this % rhs == 0) div else div + 1
}


@Serializable
data class IntDivision(
    val quotient: Int,
    val remainder: Int,
)

/**
 * Divide this by [divisor] using integer division and return both the quotient and remainder.
 *
 * Example: `7 divRem 3 == IntDivision(quotient=2, remainder=1)`
 */
infix fun Int.divRem(divisor: Int) = IntDivision(this / divisor, this % divisor)
