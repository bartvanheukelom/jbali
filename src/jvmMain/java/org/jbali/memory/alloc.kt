@file:JvmName("Alloc")

package org.jbali.memory

/**
 * Thrown when a function failed to complete due to a lack of available memory.
 * Unlike [OutOfMemoryError], this exception promises that the JVM state is still valid.
 * Note that that specifically refers to the JVM's internal state. Like any exception,
 * it cannot guarantee that all callers between the throw site and catch site have properly
 * handled the exceptional situation.
 *
 * https://stackoverflow.com/questions/72546743/outofmemoryerror-is-dangerous-but-why-more-dangerous-than-other-exceptions/72547039
 */
class InsufficientMemoryException

@JvmOverloads constructor(
    val required: Long? = null,
//    val free: Long = Runtime.getRuntime().freeMemory(),
    cause: Throwable? = null
)
    : RuntimeException("Free memory is insufficient, require ${required ?: "?"} B", cause)

/**
 * Allocates a new byte array.
 * When applicable, throws [InsufficientMemoryException] instead of [OutOfMemoryError].
 */
fun newByteArray(size: Int) =
    try {
        ByteArray(size)
    } catch (e: OutOfMemoryError) {
        throw InsufficientMemoryException(size.toLong(), e)
    }

/**
 * Allocates a new [CharArray].
 * When applicable, throws [InsufficientMemoryException] instead of [OutOfMemoryError].
 */
fun newCharArray(size: Int) =
    try {
        CharArray(size)
    } catch (e: OutOfMemoryError) {
        throw InsufficientMemoryException(size.toLong() * 2L, e)
    }