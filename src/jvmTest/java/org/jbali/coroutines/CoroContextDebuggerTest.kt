package org.jbali.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jbali.util.logger
import kotlin.test.Test
import kotlin.test.assertFalse

class CoroContextDebuggerTest {
    
    private val log = logger<CoroContextDebuggerTest>()
    
    @Test fun testSimplest() {
        val db = CoroContextDebugger(logAllSlices = true)
        db.use {
            runBlocking {
                db.execute {
                    log.info("hi")
                }
            }
        }
        // fails TODO
        // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/11101
        assertFalse(db.failed)
    }
    
    @Test fun testNestedAndDispatcher() {
        val db = CoroContextDebugger(logAllSlices = true)
        db.use {
            runBlocking {
                db.execute { ex ->
                    delay(10L)
                    ex.checkThreadContext("between delays")
                    
                    db.execute { ex2 ->
                        withContext(Dispatchers.IO) {
                            ex2.checkThreadContext("in IO")
                        }
                        ex2.checkThreadContext("between delays 2")
                    }
                    
                    delay(10L)
                }
            }
        }
        
        assertFalse(db.failed)
    }
    
}
