package org.jbali.math

/**
 * Returns the remainder of Euclidean division of this integer by [d].
 * Unlike the standard modulo operator `%`, this function always returns
 * a result in the range `[0, d)` (for positive `d`) or `(d, 0]` (for negative `d`).
 *
 * | lhs | rhs | divmod | mod |
 * |-----|-----|--------|-----|
 * |  12 |  10 |      2 |   2 |
 * |  -2 |  10 |      8 |  -2 |
 * |  12 | -10 |     -8 |   2 |
 * | -12 | -10 |     -2 |  -2 |
 */
infix fun Int.divmod(d: Int): Int =
    this.mod(d).plus(d).mod(d)
