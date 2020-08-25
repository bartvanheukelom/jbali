package org.jbali.util

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@Serializable
data class PartialFoo(
        val x: Int,
        val y: String?,
        val z: Bar
) {
    @Serializable
    data class Bar(val beer: String)
}

class PartialObjectTest {

    val whole = PartialFoo(
            x = 12,
            y = null,
            z = PartialFoo.Bar(beer = "zoeltenbrau")
    )

    @Test fun testPartialXZ() {
        val partialXZ =
                whole.partial { it.name in setOf("x", "z") }

        assertEquals("Partial<...>(x=${whole.x}, z=${whole.z})", partialXZ.toString())

        partialXZ.let {
            assertEquals(whole.x, it[PartialFoo::x])
            assertEquals(whole.z, it[PartialFoo::z])

            assertNull(it[PartialFoo::y])
            assertFailsWith<NoSuchElementException> {
                it.getValue(PartialFoo::y)
            }
        }
    }

}