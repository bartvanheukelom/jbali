package org.jbali.reflect

import kotlin.reflect.jvm.javaMethod
import kotlin.test.Test
import kotlin.test.assertEquals

interface IFoo {
    fun hi(foo: String, x: Int)
}

class MethodInvocationTest {

    @Test fun testToString() {
        assertEquals(
                "hi(foo=bar, x=2)",
                MethodInvocation(IFoo::hi.javaMethod!!, listOf("bar", 2)).toString()
        )
    }

    @Test fun testEquals() {
        assertEquals(
                MethodInvocation(IFoo::hi.javaMethod!!, listOf("bar", 2)),
                MethodInvocation(IFoo::hi.javaMethod!!, listOf("bar", 2))
        )
    }

    @Test fun testArrayConstructor() {
        assertEquals(
                MethodInvocation(IFoo::hi.javaMethod!!, arrayOf("bar", 2)),
                MethodInvocation(IFoo::hi.javaMethod!!, listOf("bar", 2))
        )
    }

}
