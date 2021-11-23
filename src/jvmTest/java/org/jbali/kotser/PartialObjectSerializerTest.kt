package org.jbali.kotser

import org.jbali.json2.JSONString
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
            JSONString("""
                {
                    "x": 12,
                    "z": {
                        "beer": "zoeltenbrau"
                    }
                }
            """),
            expectToElementToFail = true, // TODO fix
        )
    }


    @Test fun testXY() {
        js.assertSerialization(
            PartialObject.Mapped<PartialFoo>(mapOf(
                PartialFoo::x to 12,
                PartialFoo::y to null
            )),
            JSONString("""
                {
                    "x": 12,
                    "y": null
                }
            """),
            expectToElementToFail = true, // TODO fix
        )
    }

}