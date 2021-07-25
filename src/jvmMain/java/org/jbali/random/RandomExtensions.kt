package org.jbali.random

import org.jbali.bytes.asData
import org.jbali.math.powerOf10
import kotlin.random.Random

/**
 * Generate a long with at most n digits.
 * E.g. nextLongOfDigits(4) will return a number in the range [0, 9999]
 */
fun Random.nextLongOfDigits(n: Int) = nextLong(powerOf10(n))

// "tokens"

@OptIn(ExperimentalUnsignedTypes::class)
fun Random.nextBase64(chars: UInt): String {
    val bits = chars * 6u
    val bytes = bits / 8u
    return nextBytes(bytes.toInt()).asData().toBase64()
}

@OptIn(ExperimentalUnsignedTypes::class)
fun Random.nextHex(chars: UInt): String {
    require(chars % 2u == 0u)
    val bytes = chars / 2u
    return nextBytes(bytes.toInt()).asData().toHexString()
}
