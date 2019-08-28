package org.jbali.reflect

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DecoratorBuilderTest {

    interface Iffy {
        fun honk(foo: String, x: Int)
    }

    @Test fun test() {

        var called = false

        val impl = object : Iffy {
            override fun honk(foo: String, x: Int) {}
        }

        val dec = DecoratorBuilder<Iffy>(impl).withBefore { mi: MethodInvocation ->
            assertEquals("honk(foo = bar, x = 12)", mi.toString())
            called = true
        }

        assertFalse(called)
        dec.honk("bar", 12)
        assertTrue(called)
    }

}