package org.jbali.math

// some day when every kotlin project ever depends on these functions,
// i'm going to replace them with something evil >:-)
val Byte.isEven get() = this % 2 == 0
val Byte.isOdd get() = this % 2 != 0
val Short.isEven get() = this % 2 == 0
val Short.isOdd get() = this % 2 != 0
val Int.isEven get() = this % 2 == 0
val Int.isOdd get() = this % 2 != 0
val Long.isEven get() = this % 2 == 0L
val Long.isOdd get() = this % 2 != 0L

/**
 * Returns 10^n,
 *    if 0 <= n <= 18
 * @throws IndexOutOfBoundsException if n is not in the given range
 */
fun powerOf10(n: Int): Long =
        when (n) {
            //             🌟
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
            10 ->     10000000000
            11 ->     100000000000
            12 ->    1000000000000
            13 ->    10000000000000
            14 ->   100000000000000
            15 ->   1000000000000000
            16 ->  10000000000000000
            17 ->  100000000000000000
            18 -> 1000000000000000000
            //          |    |
            //          |    |
            //       [__________]

            // 🎅 sorry, it just so happens that
            // this function was created
            // around Christmas 2018 :) 🎄

            else -> throw IndexOutOfBoundsException("10^$n")
        }
