package org.jbali.bytes

import org.apache.commons.codec.binary.Hex
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Bytes4Test {

    @Test fun test() {

        val b = ByteArray(4)
        b[1] = 100
        assertNotEquals(0, Bytes4(b).getInt())

        val x = Bytes4()
        assertEquals(0, x.getInt())

        val yNum = 10
        val y = Bytes4(yNum)
        assertEquals(yNum, y.getInt())

        val zNum = 345783481

        fun getNativeInt(name: String, arr: ByteArray, offset: Int = 0): Int =
            ByteBuffer.wrap(arr, offset, 4).let {
                it.order(ByteOrder.BIG_ENDIAN)
                val be = it.getInt(offset)
                it.order(ByteOrder.nativeOrder())
                val na = it.getInt(offset)
                println("${name.padEnd(10)} BE=$be NA=$na")
                na
            }

        val z = Bytes4(zNum)
        assertEquals(zNum, getNativeInt("z.array", z.array))
        assertEquals(zNum, z.getInt())
        assertEquals(yNum xor zNum, (y xor z).getInt())

        val pad = 12

        val zz: ByteArray = ByteArrayOutputStream().let {
            it.write(ByteArray(pad))
            it.write(z.array)
            it.toByteArray()
        }
        assertEquals(pad+4, zz.size)
        println(Hex.encodeHexString(zz))
        assertEquals(zNum, getNativeInt("zz", zz, pad))
        assertEquals(yNum xor zNum, (y.xor(zz, pad)).getInt())

        zz.xor4ip(pad, y)
        assertEquals(yNum xor zNum, getNativeInt("zz xorip", zz, pad))

        // the following would crash:
        // println(theUnsafe.getInt(zz, 10000000000L))

    }

}