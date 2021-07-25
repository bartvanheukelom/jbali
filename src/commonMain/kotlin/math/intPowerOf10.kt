package org.jbali.math


/**
 * Returns 10^n,
 *    if 0 <= n <= 9
 *
 * Use `powerOf10(...): Long` for more range.
 *
 * @throws IndexOutOfBoundsException if n is not in the given range
 */
fun intPowerOf10(n: Int): Int =
        when (n) {
            //            🌟
            0 ->          1
            1 ->          10
            2 ->         100
            3 ->         1000
            4 ->        10000
            5 ->        100000
            6 ->       1000000
            7 ->       10000000
            8 ->      100000000
            9 ->      1000000000
            //          |    |
            //          |    |
            //       [__________]

            // 🎅 sorry, it just so happens that
            // the Long version of this function was created
            // around Christmas 2018 :) 🎄

            else -> throw IndexOutOfBoundsException("10^$n")
        }
