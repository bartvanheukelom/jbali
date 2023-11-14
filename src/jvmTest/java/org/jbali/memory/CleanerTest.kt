package org.jbali.memory

import kotlinx.coroutines.*
import org.jbali.coroutines.awaitFor
import org.jbali.random.nextHex
import org.jbali.util.NanoDuration
import org.jbali.util.NanoTime
import org.jbali.util.logger
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.ref.Cleaner
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference
import javax.management.ObjectName
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CleanerTest {
    
    private var r1: Any? = null
    
    // comment out @Test when not in use, as the result of this test is meaningless and can take a long time
//    @Test
    fun testGlobalCleanerManual() {
        log.info("----------------------------- testGlobalCleanerManual -----------------------------")
        run {
            val o = StickyObject()
            o.pretendUsage()
            log.info("registering StickyObject() cleaner")
            globalCleaner.register(o) {
                log.info("cleaning StickyObject()")
            }
        }
        repeat(10) {
            gcForced()
            Thread.sleep(1000)
        }
    }
    
    @Test fun testGlobalCleaner() {
        log.info("----------------------------- testGlobalCleaner -----------------------------")
        log.info("globalCleaner=$globalCleaner")
        val oCleaned = CompletableDeferred<Unit>()
        tempSticky {
            log.info("oCleaned.active=${oCleaned.isActive} oCleaned.completed=${oCleaned.isCompleted}")
            log.info("going to complete oCleaned")
            oCleaned.complete(Unit)
            log.info("oCleaned.active=${oCleaned.isActive} oCleaned.completed=${oCleaned.isCompleted}")
        }
        try {
            runBlocking {
                val gcLoop = launchGcLoop()
                try {
                    oCleaned.awaitFor(10.seconds)
                    log.info("oCleaned.awaitFor completion received in runBlocking")
                    gcLoop.cancelAndJoin()
                    log.info("gcLoop cancelled and joined")
                } catch (e: CancellationException) {
                    log.warn("oCleaned.awaitFor cancelled, cancelling runBlocking")
                    cancel(e)
                }
                log.info("end of runBlocking")
            }
            log.info("left runBlocking")
        } catch (e: TimeoutCancellationException) {
            log.warn("oCleaned not completed after timeout, is StickyObject() still alive? triggering a heap dump...")
            dumpHeapToFile()
            throw e
        }
    }
    
    @Test fun testCloser() {
        log.info("----------------------------- testGlobalCleanerCloser -----------------------------")
        
        val r1c = Resourz("r1").also { r1 = it }.child
        
        assertEquals(null, r1c.get())
        gc()
        assertEquals(null, r1c.get())
        r1 = null
        gc()
        
        val start = NanoTime.now()
        var delay = 10L
        while (r1c.get() == null) {
            if (NanoDuration.since(start) > NanoDuration(10_000_000_000)) {
                dumpHeapToFile()
                throw AssertionError("r1c still not cleaned after timeout")
            }
            delay *= 2
            log.info("r1c still not cleaned, waiting $delay ms")
            Thread.sleep(delay.coerceAtMost(1280L))
            gc()
        }
        assertEquals(true, r1c.get())
        
    }
    
    // comment out @Test when not in use, see above
//    @Test
    fun testCloserManual() {
        log.info("----------------------------- testCloserManual -----------------------------")
        run {
            val o = Resourz("r1")
        }
        repeat(10) {
            gc()
            Thread.sleep(1000)
        }
    }
    
    
    
    // ----------------------- helpers ----------------------- //
    
    companion object {
        
        private val log = logger<CleanerTest>()
        
        fun gc() {
            log.info("Triggering GC")
            gcForced()
        }
        
        fun dumpHeapToFile(test: String = "CleanerTest") {
            val dumpName = File("build/heapdump_${test}_${System.currentTimeMillis()}.hprof").absolutePath
            ManagementFactory.getPlatformMBeanServer().invoke(
                ObjectName("com.sun.management:type=HotSpotDiagnostic"),
                "dumpHeap",
                arrayOf<Any?>(dumpName, true),
                arrayOf("java.lang.String", "boolean")
            )
            log.warn("heap dumped to file://$dumpName")
        }
    }
    
    
    private fun tempSticky(onClean: () -> Unit): Cleaner.Cleanable? {
        val o = StickyObject("SO-${Random.nextHex(4u)}")
        val n = o.name
        o.pretendUsage()
        log.info("registering $o cleaner")
        return globalCleaner.register(o) {
            log.info("cleaning $n")
            onClean()
        }
    }
    
    
}


/**
 * Simple class with some ballast.
 */
private data class StickyObject(
    val name: String = "StickyObject",
) {
    // make it have some weight
    private val buf = ByteBuffer.allocate(1024 * 1024 * 16)
    
    /**
     * Call this at least somewhere to prevent the compiler from optimizing away the unused buffer (don't think it would, but just in case).
     */
    fun pretendUsage() {
        if (Random.nextLong() == 0L) { // prevent optimization
            println(buf[456])
        }
    }
}


/**
 * Class with a fake child resource, that registers a cleaner.
 */
private data class Resourz(
    val name: String,
) : AutoCloseable {
    companion object {
        private val log = logger<Resourz>()
    }
    
    /**
     * By storing a reference to this, you can assert whether the cleaner has been invoked,
     * without having to store and keep alive this Resourz itself.
     */
    val child = AtomicReference<Boolean?>()
    private val closer = run {
        val n = "$this"
        val c = child
        globalCleaner.registerCloser(this) { cleaning ->
            log.info("$n's closer invoked, cleaning=$cleaning")
            c.set(cleaning)
        }
    }
    override fun close() {
        log.info("$this.close()")
        closer.close()
    }
}


/**
 * Launch a looping coroutine that calls [gcForced] every 100 ms.
 */
fun CoroutineScope.launchGcLoop() =
    launch {
        while (true) {
            println("gcLoop going to call gcForced()")
            gcForced()
            try {
                delay(100)
            } catch (e: CancellationException) {
                println("gcLoop cancelled")
                throw e
            }
        }
    }


/**
 * Force GC by allocating a buffer that is as big as the max heap size.
 * This will trigger an [OutOfMemoryError] which is caught and ignored.
 * Also calls [System.gc] afterwards.
 *
 * @throws AssertionError if the OOM is not triggered
 */
fun gcForced() {
    val r = Runtime.getRuntime()
//    val free = r.freeMemory()
    val total = r.maxMemory()
    try {
        val buf = ByteBuffer.allocate(total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        if (Random.nextLong() == 0L) { // prevent optimization
            println(buf[456])
        }
        throw AssertionError("Failed to trigger OOM")
    } catch (oom: OutOfMemoryError) {}
    
    // just in case
    System.gc()
}
