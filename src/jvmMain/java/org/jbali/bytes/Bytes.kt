package org.jbali.bytes

import sun.misc.Unsafe
import kotlin.experimental.xor

@JvmField
val theUnsafe: Unsafe =
        Unsafe::class.java.getDeclaredField("theUnsafe").let {
            it.isAccessible = true
            it.get(null) as Unsafe
        }
@JvmField
val byteArrOffset = Unsafe.ARRAY_BYTE_BASE_OFFSET.toLong()

infix fun ByteArray.xor(mask: ByteArray): ByteArray {
    return if (size == 4) {
        check(mask.size >= 4)
        (Bytes4(this) xor Bytes4(mask)).array
    } else {
        check(mask.size > size)
        mapBytes { i, b -> b xor mask[i] }
    }
}

inline fun ByteArray.mapBytes(m: (Int, Byte) -> Byte) =
    ByteArray(size) { i ->
        m(i, this[i])
    }
