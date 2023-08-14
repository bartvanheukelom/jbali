@file:OptIn(ExperimentalTime::class)

package org.jbali.coroutines

import kotlinx.coroutines.*
import org.jbali.util.logger
import kotlin.concurrent.thread
import kotlin.test.*
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
    
    @Test fun testRunBlockingInterruptable() {
        lateinit var ex: Throwable
        var delaying = false
        val th = thread {
            log.info("started thread")
            try {
                runBlockingInterruptable {
                    log.info("started runBlockingInterruptable")
                    try {
                        delaying = true
                        
                        // <race condition if this thread is interrupted at this point>
                        
                        delayIndefinitely<Unit>()
                    } catch (e: Throwable) {
                        log.info("caught exception from delayIndefinitely", e)
                        throw e
                    }
                    delayIndefinitely<Unit>()
                }
            } catch (e: Throwable) {
                log.info("caught exception from runBlockingInterruptable", e)
                ex = e
            }
        }
        while (!delaying) {
            Thread.sleep(1)
        }
        Thread.sleep(50) // TODO use a latch
        log.info("coroutine ready for interruption")
        
        log.info("interrupting thread")
        th.interrupt()
        log.info("joining thread")
        th.join()
        log.info("joined thread")
        
        assertTrue(ex.stackTrace.any { it.methodName == "delayIndefinitely" })
        
    }
    
}