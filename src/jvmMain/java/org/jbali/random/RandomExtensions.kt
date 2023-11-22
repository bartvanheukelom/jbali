package org.jbali.random

import org.jbali.bytes.asData
import org.jbali.math.powerOf10
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or
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

/**
 * Generate a UUID with the version 4 (random) and variant 0b10x (RFC 4122).
 * This is what [UUID.randomUUID] does, but that method does not allow choosing the random source.
 */
fun Random.nextUUID(): UUID {
    val bytes = ByteBuffer.wrap(nextBytes(16))
    bytes.put(6, bytes.get(6) and 0b0000_1111)           // bits[48..51] = 0b0000
    bytes.put(6, bytes.get(6) or  0b0100_0000)           // bits[48..51] = 0b0100 = version 4
    bytes.put(8, bytes.get(8) and 0b0011_1111)           // bits[64..65] = 0b00
    bytes.put(8, bytes.get(8) or  0b1000_0000u.toByte()) // bits[64..65] = 0b10 = variant 2
    return UUID(bytes.long, bytes.long)
}
