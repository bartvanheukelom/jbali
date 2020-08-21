package org.jbali.ktor

import io.ktor.http.HttpMethod
import io.ktor.http.RequestConnectionPoint
import org.jbali.util.FakeExpression
import org.jbali.util.forEachWrappingExceptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class HTTPOriginTest {

    @Test fun test() {

        val httpExample = HTTPOrigin("http://example.com")
        val httpExample80 = HTTPOrigin("http://example.com:80")
        assertEquals(httpExample, httpExample80)

        val httpExample99 = HTTPOrigin("http://example.com:99")
        assertEquals("http://example.com:99", httpExample99.string)

        val httpFoo = HTTPOrigin("http://foo.bar")
        assertNotEquals(httpExample, httpFoo)

        val httpsExample = HTTPOrigin("https://example.com")
        assertEquals("https://example.com", httpsExample.string)
        assertNotEquals(httpExample, httpsExample)

        val httpsExample443 = HTTPOrigin("https://example.com:443")
        assertEquals("https://example.com", httpsExample443.string)
        assertEquals(httpsExample, httpsExample443)

        val httpsExample555 = HTTPOrigin("https://example.com:555")
        assertEquals("https://example.com:555", httpsExample555.string)

    }

    @Test fun testRCE() {
        data class RCE(
                override val scheme: String,
                override val host: String,
                override val port: Int
        ) : RequestConnectionPoint {
            override val method: HttpMethod get() = FakeExpression.errorForTest()
            override val remoteHost: String get() = FakeExpression.errorForTest()
            override val uri: String get() = FakeExpression.errorForTest()
            override val version: String get() = FakeExpression.errorForTest()
        }

        assertEquals("http://example.com", RCE("http", "example.com", 80).httpOrigin.string)
        assertEquals("http://example.com:81", RCE("http", "example.com", 81).httpOrigin.string)
        assertEquals("https://example.com", RCE("https", "example.com", 443).httpOrigin.string)
    }

    @Test fun testIllegalUrls() {
        listOf(
                "http://example.com/",
                "http://example.com/foo",
                "http://example.com?",
                "http://example.com?bla",
                "http://henk@example.com",
                "http://henk:pass@example.com"
        ).forEachWrappingExceptions {
            assertFailsWith<IllegalArgumentException> {
                HTTPOrigin(it)
            }
        }
    }

}
