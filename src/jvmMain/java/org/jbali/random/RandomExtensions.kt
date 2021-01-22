package org.jbali.random

import org.jbali.math.powerOf10
import kotlin.random.Random

/**
 * Generate a long with at most n digits.
 * E.g. nextLongOfDigits(4) will return a number in the range [0, 9999]
 */
fun Random.nextLongOfDigits(n: Int) = nextLong(powerOf10(n))

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
            toString()
        }

fun Random.nextDigit(): Char =
    nextInt('0'.toInt(), '9'.toInt()).toChar()

fun Random.nextUpperCaseLetter(): Char =
    nextInt('A'.toInt(), 'Z'.toInt()).toChar()
