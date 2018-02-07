package org.jbali.util

import org.junit.Test
import kotlin.test.*

typealias P = RelativePath

class RelativePathTest {

    @Test fun testGeneral() {

        assertTrue(P().empty)
        assertNull(P().optString)

        val hw = P("hello", "world")
        assertFalse(hw.empty)
        assertEquals("hello", hw.first)
        assertEquals("world", hw.last)
        assertEquals("hello/world", hw.optString)

        // with slashes
        assertEquals(hw, P("hello/world"))
        assertEquals("a/b/c", P("a/b", "c").toString())

        assertEquals(P("login"), P("login"))

        assertTrue(P("login").equalTo("login"))
        assertTrue(P("secure", "thing").startsWith("secure"))
    }

    @Test fun testSub() {
        assertEquals(P("bar"), P("foo", "bar").subPath(1))
        assertEquals(P(), P("foo", "bar").subPath(2))

        assertEquals(P(), P("secure").subIfStartsWith("secure"))
        assertEquals(P("thing", "bla"), P("secure", "thing", "bla").subIfStartsWith("secure"))
        assertEquals(P("thing", "bla"), P("secure", "thing", "bla").subIfStartsWith(P("secure")))
        assertNull(P("hey", "there").subIfStartsWith("secure"))
    }

    @Test fun testNormalize() {

        assertEquals(P("hi", "guy"), P("./hi/girl/../guy").dotNormalized())

        assertFailsWith(IllegalArgumentException::class) {
            P("too/deep/../../..").dotNormalized()
        }

    }

}