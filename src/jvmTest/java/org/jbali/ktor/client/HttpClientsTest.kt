package org.jbali.ktor.client

import org.jbali.util.OneTimeFlag
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpClientsTest {
    
    @Test
    fun create() {
        
        var closed by OneTimeFlag()
        HttpClients.create("test", onClosed = { closed = true }).use {
            assertFalse(closed)
        }
        assertTrue(closed)
        
    }
}