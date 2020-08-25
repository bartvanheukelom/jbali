package org.jbali.kotser

import org.jbali.util.PartialFoo
import org.jbali.util.PartialObject
import kotlin.test.Test

class PartialObjectSerializerTest {

    val js: JsonSerializer<PartialObject<PartialFoo>> = jsonSerializer()

    @Test fun testEmpty() {
        js.assertSerialization(
                PartialObject.Mapped<PartialFoo>(emptyMap()),
                """
                    {}
                """.trimIndent()
        )
    }

    @Test fun testXZ() {
        js.assertSerialization(
                PartialObject.Mapped<PartialFoo>(mapOf(
                        PartialFoo::x to 12,
                        PartialFoo::z to PartialFoo.Bar("zoeltenbrau")
                )),
                """
                    {
                        "x": 12,
                        "z": {
                            "beer": "zoeltenbrau"
                        }
                    }
                """.trimIndent()
        )
    }


    @Test fun testXY() {
        js.assertSerialization(
                PartialObject.Mapped<PartialFoo>(mapOf(
                        PartialFoo::x to 12,
                        PartialFoo::y to null
                )),
                """
                    {
                        "x": 12,
                        "y": null
                    }
                """.trimIndent()
        )
    }

}