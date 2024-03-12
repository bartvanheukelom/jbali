package org.jbali.math

/**
 * Represents the abstract number 0. Can be compared to any other [Number],
 * with this object's [compareTo] method if used as the left-hand operand,
 * or the `Number.compareTo(Zero)` extension method if used as the right-hand operand.
 */
data object Zero : Comparable<Number>, Number() {
    
    override fun compareTo(other: Number) = (0.0).compareTo(other.toDouble())
    
    override fun toByte(): Byte = 0
    override fun toDouble(): Double = 0.0
    override fun toFloat(): Float = 0.0f
    override fun toInt(): Int = 0
    override fun toLong(): Long = 0
    override fun toShort(): Short = 0
    
    override fun toString() = "0"
    
}

operator fun Number.compareTo(other: Zero) = -(other.compareTo(this))

// the above can be hard to auto-import, so we also provide:
val Number.isPositive get() = this > Zero
val Number.isNegative get() = this < Zero
