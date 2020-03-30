package org.jbali.reflect

import kotlin.reflect.jvm.javaMethod
import kotlin.test.*

class DecoratorBuilderTest {

    interface Iffy {
        fun honk(foo: String, x: Int)
    }

    @Test fun test() {

        var receivedInvocation: MethodInvocation? = null

        val impl =
                object : Iffy {
                    override fun honk(foo: String, x: Int) {}
                }

        val dec =
                DecoratorBuilder<Iffy>(impl).withBefore { mi: MethodInvocation ->
                    receivedInvocation = mi
                }

        assertNull(receivedInvocation)

        dec.honk("foo", 1)
        assertEquals(MethodInvocation(
                method = Iffy::honk.javaMethod!!,
                args = listOf("foo", 1)
        ), receivedInvocation)

        dec.honk("bar", 12)
        assertEquals(MethodInvocation(
                method = Iffy::honk.javaMethod!!,
                args = listOf("bar", 12)
        ), receivedInvocation)
    }

}