package org.jbali.kotser

import kotlinx.serialization.Serializable
import org.jbali.json2.KVON
import kotlin.test.Test
import kotlin.test.assertEquals

class KVONDeserializerTest {

    @Serializable
    data class Obj(
            val x: Int,
            val y: Int
    )

    @Serializable
    data class Foo(
            val num: Int,
            val str: String,
            val cool: Boolean,
            val obj: Obj,
            val list: List<Obj>,
            val optional: String = "hello"
    )

    private fun fooReader() =
            KVONDeserializer(
                    deserializer = Foo.serializer()
            ) {
                println(it)
            }

    private val fooInput = KVON.Pairs(
            "num" to "12",
            "str" to "\"hi\"",
            "cool" to "false",
            "obj" to """{"x": 1, "y": 2}""",
            "list" to """[{"x": 1, "y": 2}, {"x": 3, "y": 4}]"""
    )

    @Test
    fun testFoo() {
        assertEquals(
                expected = Foo(
                        num = 12,
                        str = "\"hi\"",
                        cool = false,
                        obj = Obj(x = 1, y = 2),
                        list = listOf(
                                Obj(x = 1, y = 2),
                                Obj(x = 3, y = 4)
                        ),
                        optional = "hello"
                ),
                actual = fooReader().deserialize(fooInput)
        )
    }

}