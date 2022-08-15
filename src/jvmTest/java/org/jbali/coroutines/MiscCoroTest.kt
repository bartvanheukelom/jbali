@file:OptIn(ExperimentalTime::class)

package org.jbali.coroutines

import kotlinx.coroutines.*
import org.jbali.util.logger
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

class MiscCoroTest {
    
    private val log = logger<MiscCoroTest>()
    
    @Test fun testAwaitOrCancel() {
        runBlocking {
            
            suspend fun startLongTask(seq: String): Deferred<Unit> {
                val started = CompletableDeferred<Unit>()
                val def = async {
                    log.info("start infinitely long task $seq")
                    started.complete(Unit)
                    try {
                        delayIndefinitely<Unit>()
                    } catch (ce: CancellationException) {
                        log.info("task $seq cancelled")
                        throw ce
                    }
                }
                started.await()
                return def
            }
            
            
            log.info("-> default behaviour is not to cancel")
            run {
                val def = startLongTask("default")
                assertFailsWith<TimeoutCancellationException> {
                    withTimeout(1.milliseconds) { def.await() }
                }
                assertTrue(def.isActive)
                // cancel manually or the test will hang
                def.cancel()
            }
            
            log.info("-> awaitFor uses awaitOrCancel which cancels the deferred")
            run {
                val def = startLongTask("awaitFor")
                assertFailsWith<TimeoutCancellationException> {
                    def.awaitFor(1.milliseconds)
                }
                assertFalse(def.isActive)
                assertTrue(def.isCancelled)
//                assertTrue(def.isCompleted) - TODO when is it
            }
    
            log.info("-> cancelling the await manually")
            run {
                val def = startLongTask("awaitOrCancel")
                
                val waiter = async {
                    assertFailsWith<CancellationException> {
                        def.awaitOrCancel()
                    }.also { e ->
                        log.info("awaitOrCancel failed", e)
                        throw e
                    }
                }
                
                yield()
                assertTrue(waiter.isActive)
                log.info("going to cancel waiter")
                waiter.cancelAndJoin()
                log.info("waiter joined")
                assertTrue(waiter.isCancelled)
                assertTrue(def.isCancelled)
            }
            
            // if we left any children running, runBlocking won't return
            currentCoroutineContext().job.children.forEach {
                assertFalse(it.isActive, "$it is active")
            }
            log.info("leaving runBlocking")
        }
        log.info("left runBlocking")
    }
    
}