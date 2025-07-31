package org.jbali.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jbali.util.logger
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

class CoroContextDebuggerTest {
    
    private val log = logger<CoroContextDebuggerTest>()
    
    @Test fun testSimplest() {
        val db = CoroContextDebugger(logAllSlices = true,
            // throwOnFail = true  TODO enable when bug fixed
        )
        db.use {
            runBlocking {
                db.execute {
                    log.info("hi")
                }
            }
        }
        // currently known to fail, see:
        // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/11101
        // https://youtrack.jetbrains.com/issue/KTOR-6118/CallMonitoring-SuspendFunctionGun-sometimes-leaks-coroutine-context
        // TODO enable check when bug fixed
//        assertNull(db.failed.flaggedSince)
    }
    
    @Test fun testNestedAndDispatcher() {
        val db = CoroContextDebugger(logAllSlices = true,
            // throwOnFail = true
        )
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
        // see above
//        assertNull(db.failed.flaggedSince)
    }
    
}
