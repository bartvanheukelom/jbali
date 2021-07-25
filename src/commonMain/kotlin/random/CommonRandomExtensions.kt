package org.jbali.random

import org.jbali.math.NormalDistribution
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sqrt
import kotlin.random.Random


// whole numbers with a given number of decimal digits

private const val DIGIT_CHARS = "0123456789"

/**
 * Generate a random number string with an exact number
 * of decimal digits, including padding zeroes.
 * E.g. nextPaddedNum(4) -> "1749",
 *      nextPaddedNum(5) -> "00212"
 */
fun Random.nextPaddedNum(digits: Int): String =
        CharArray(digits).let { a ->
            for (d in 0 until digits) {
                a[d] = DIGIT_CHARS.random(this)
            }
            a.concatToString()
        }


// characters

@OptIn(ExperimentalStdlibApi::class)
fun Random.nextDigit(): Char =
    nextInt('0'.code, '9'.code).toChar()

@OptIn(ExperimentalStdlibApi::class)
fun Random.nextUpperCaseLetter(): Char =
    nextInt('A'.code, 'Z'.code).toChar()


// numbers on normal distribution

fun Random.nextNormalDouble(): Double =
    sqrt(-2 * log10(nextDouble())) * cos(2 * PI * nextDouble())

fun Random.nextNormalDouble(mean: Double, sd: Double): Double =
    (nextNormalDouble() * sd) + mean

fun Random.nextNormalDouble(distribution: NormalDistribution): Double =
    nextNormalDouble(distribution.mean, distribution.sd)
