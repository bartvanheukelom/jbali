package org.jbali.random

import kotlin.random.Random

/**
 * Expose a java Random as a kotlin Random.
 * (Copied from kotlin.random.AbstractPlatformRandom, which is internal)
 */
open class JavaRandomAsKotlin(
    val java: java.util.Random
) : Random() {

    private fun Int.takeUpperBits(bitCount: Int): Int =
        this.ushr(32 - bitCount) and (-bitCount).shr(31)

    override fun nextBits(bitCount: Int): Int =
        java.nextInt().takeUpperBits(bitCount)

    override fun nextInt(): Int = java.nextInt()
    override fun nextInt(until: Int): Int = java.nextInt(until)
    override fun nextLong(): Long = java.nextLong()
    override fun nextBoolean(): Boolean = java.nextBoolean()
    override fun nextDouble(): Double = java.nextDouble()
    override fun nextFloat(): Float = java.nextFloat()
    override fun nextBytes(array: ByteArray): ByteArray = array.also { java.nextBytes(it) }

}

val java.util.Random.kotlin: Random
    get() = JavaRandomAsKotlin(this)
