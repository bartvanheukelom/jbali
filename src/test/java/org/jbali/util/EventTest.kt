package org.jbali.util

import org.slf4j.LoggerFactory
import kotlin.test.*

data class Payload(val v: String) {
    companion object {
        val e = Payload("")
        val hello = Payload("hello")
        val goodbye = Payload("goodbye")
    }
}

sealed class EventTest {

    override fun toString() = "ET"

    val evNoName = Event<Payload>()
    val evExpName = Event<Payload>("HelloWorld")
    val evPropName: Event<Payload> = Event(this::evPropName)
    val evPropName2: Event<Payload> = Event("Foo", this::evPropName2)
    val evPropName3: Event<Payload> = Event(EventTest::class, this::evPropName3)
    var evPropName4: Event<Payload> = Event(this::evPropName4)
    val evDelegate by EventDelegate<Payload>()

    val evOnce by OnceEventDelegate<Payload>()

}

class EventTestChild : EventTest() {

    override fun toString() = "ETC"

    @Test fun testEventNames() {
        assertNull(evNoName.name)
        assertEquals("HelloWorld", evExpName.name)
        assertEquals("org.jbali.util.EventTest.evPropName", evPropName.name)
        assertEquals("[Foo].evPropName2", evPropName2.name)
        assertEquals("org.jbali.util.EventTest.evPropName3", evPropName3.name)
        assertEquals("org.jbali.util.EventTest.evPropName4", evPropName4.name)
        assertEquals("[ETC].evDelegate", evDelegate.name)

        assertSame(evDelegate, evDelegate)
    }

    private fun throwing(x: Any): Nothing = throw IllegalStateException(x.toString())

    @Test fun testEvents() {

        println(evDelegate)

        var x = Payload.e
        var y = Payload.e
        val lstX = evDelegate.listen { x = it }
        evDelegate.listen { y = Payload("B$it") }

        val lstThatThrows = evDelegate.listen(::throwing)
        for (i in 0..100) evDelegate.listen {}

        assertEquals("AmazingTestListener", evDelegate.listen("AmazingTestListener") {}.name)

        var caughtError: Pair<EventListener<Payload>, Throwable>? = null
        evDelegate.dispatch(Payload.hello) { l, e ->
            println("Error in listener ${l.name}")
            caughtError = Pair(l, e)
        }

        assertEquals(Payload.hello, x)
        assertEquals(Payload("B${Payload.hello.v}"), y)

        assertEquals(lstThatThrows, caughtError!!.first)
        assertTrue(caughtError!!.second is IllegalStateException)

        lstX.detach()
        evDelegate.dispatch(Payload.goodbye)
        assertEquals(Payload.goodbye, x)
        assertEquals(Payload("B${Payload.hello.v}"), y)

    }

    @Test fun once() {
        var x = Payload.e
        val listener = evOnce.listen { x = it }

        fun expectISE(block: () -> Unit) {
            assertFailsWith<IllegalStateException> {
                block()
            }
        }

        evOnce.listen {
            // cannot attach during dispatch
            expectISE { evOnce.listen { p -> println(p) } }
        }

        assertTrue(listener.attached)

        evOnce.dispatch(Payload.hello)
        assertEquals(x, Payload.hello)
        assertFalse(listener.attached)

        // can no longer listen
        expectISE { evOnce.listen { println(it) } }
        expectISE { evOnce.listen("foo") { println(it) } }
        expectISE { evOnce.listenVoid(Runnable { println("yo") }) }

        // can no longer dispatch
        expectISE { evOnce.dispatch(Payload.goodbye) }
        expectISE { evOnce.dispatch(Payload.goodbye, LoggerFactory.getLogger("")) }

    }

}