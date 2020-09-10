package org.jbali.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParseCharacterSetKtTest {

    @Test fun test() {

        assertEquals("".toSortedSet(), parseCharacterSet(""))
        assertEquals("a".toSortedSet(), parseCharacterSet("a"))

        assertEquals("abcABC_-".toSortedSet(), parseCharacterSet("abcABC_-"))
        assertEquals("abcABC_-".toSortedSet(), parseCharacterSet("ABCabc_-"))
        assertEquals("abcABC_-".toSortedSet(), parseCharacterSet("a-cA-C_-"))
        assertEquals("abcABC_-".toSortedSet(), parseCharacterSet("-a-cA-C_"))
        assertEquals("abcABC_-".toSortedSet(), parseCharacterSet("-A-Ca-c_"))
        assertEquals("abcABC_-".toSortedSet(), parseCharacterSet("-A-C_a-c_"))

        assertEquals("ABC012".toSortedSet(), parseCharacterSet("A-C0-2"))
        assertEquals("-ABC".toSortedSet(), parseCharacterSet("--A-C"))

        assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_- ".toSortedSet(), parseCharacterSet("A-Za-z0-9_ -"))

        assertFailsWith<IllegalArgumentException> { parseCharacterSet("A-C--") }
        assertFailsWith<IllegalArgumentException> { parseCharacterSet("C-A") }
        assertFailsWith<IllegalArgumentException> { parseCharacterSet("A-Za-z0-9_- ") }
    }

}