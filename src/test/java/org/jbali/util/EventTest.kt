package org.jbali.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventTest {

    private val evNoName = Event<String>()
    private val evExpName = Event<String>("HelloWorld")
    private val evPropName: Event<String> = Event(this::evPropName)
    private val evPropName2: Event<String> = Event("Foo", this::evPropName2)
    private val evPropName3: Event<String> = Event(EventTest::class, this::evPropName3)
    private var evPropName4: Event<String> = Event(this::evPropName4)
    private val evDelegate by EventDelegate<String>()

    @Test fun testEventNames() {
        assertNull(evNoName.name)
        assertEquals("HelloWorld", evExpName.name)
        assertEquals("org.jbali.util.EventTest.evPropName", evPropName.name)
        assertEquals("Foo.evPropName2", evPropName2.name)
        assertEquals("org.jbali.util.EventTest.evPropName3", evPropName3.name)
        assertEquals("org.jbali.util.EventTest.evPropName4", evPropName4.name)
        assertEquals("org.jbali.util.EventTest.evDelegate", evDelegate.name)

        assertTrue(evDelegate === evDelegate)
    }

    private val onFoo: Event<String> = Event(this::onFoo)

    fun throwing(x: String): Nothing = throw IllegalStateException(x)

    @Test fun testEvents() {

        println(onFoo)

        var x = ""
        var y = ""
        val lstX = onFoo { x = it }
        onFoo { y = "B$it" }

        val lstThatThrows = onFoo(::throwing)
        for (i in 0..100) onFoo {}

        assertEquals("AmazingTestListener", onFoo.listen("AmazingTestListener", {}).name)

        var caughtError: Pair<EventListener<String>, Throwable>? = null
        onFoo.dispatch("hello", { l, e ->
            println("Error in listener ${l.name}")
            caughtError = Pair(l, e)
        })

        assertEquals("hello", x)
        assertEquals("Bhello", y)

        assertEquals(lstThatThrows, caughtError!!.first)
        assertTrue(caughtError!!.second is IllegalStateException)

        lstX.detach()
        onFoo.dispatch("goodbye")
        assertEquals("hello", x)
        assertEquals("Bgoodbye", y)

    }

}