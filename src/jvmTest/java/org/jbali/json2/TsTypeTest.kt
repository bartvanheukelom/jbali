package org.jbali.json2

import kotlin.test.Test
import kotlin.test.assertEquals

class TsTypeTest {
    
    @Test
    fun testTsArray() {
        val tsArray = TsArray(TsString)
        assertEquals("string[]", tsArray.toString())
    }
    
    @Test
    fun testTsObject() {
        val tsObject = TsObject(mapOf("prop1" to TsNumber, "prop2" to TsBoolean))
        val expected = """
            {
                prop1: number;
                prop2: boolean;
            }
        """.trimIndent()
        assertEquals(expected, tsObject.toString())
    }
    
    @Test
    fun testTsUnion() {
        val tsUnion = TsString u TsNumber u TsNull
        assertEquals("string | number | null", tsUnion.toString())
    }
    
    @Test
    fun testTsIntersection() {
        val tsIntersection = TsString i TsNumber
        assertEquals("string & number", tsIntersection.toString())
    }
    
    @Test
    fun testTsNamedTypeToDefinitionsFile() {
        val tsNamedType = TsNamedType("MyType", TsNumber)
        assertEquals("export type MyType = number;\n", tsNamedType.toDefinitionsFile())
    }
    
    @Test
    fun testMultiDefinitionsFile() {
        val foo = TsNamedType("Foo", TsObject(mapOf("prop1" to TsNumber, "prop2" to TsBoolean)))
        val bar = TsNamedType("Bar", TsObject(mapOf("fa" to foo, "fb" to foo)))
        val foobar = (foo u bar).named("FooBar")
        val expected = """
            export type FooBar = Foo | Bar;
            export type Foo = {
                prop1: number;
                prop2: boolean;
            };
            export type Bar = {
                fa: Foo;
                fb: Foo;
            };
            
        """.trimIndent()
        assertEquals(expected, foobar.toDefinitionsFile())
    }
}

fun assertEqualsIgnoreWhitespace(expected: String, actual: String) {
    assertEquals(expected.replace("\\s".toRegex(), ""), actual.replace("\\s".toRegex(), ""))
}
