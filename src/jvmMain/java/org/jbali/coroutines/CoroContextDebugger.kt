package org.jbali.coroutines

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.withContext
import org.jbali.random.nextHex
import org.jbali.text.toTableString
import org.jbali.util.OneTimeFlag
import org.jbali.util.logger
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

class CoroContextDebugger(
    val throwOnFail: Boolean = false,
    val logAllSlices: Boolean = false,
) : AutoCloseable {
    
    companion object {
        val log = logger<CoroContextDebugger>()
        val threadCode: ThreadLocal<String?> = ThreadLocal()
        
        val seq = AtomicLong()
        
        // cache<code, list<slice>>, expire 10 minutes after write
        val contextHistory: Cache<String, MutableList<ContextSlice>> = CacheBuilder.newBuilder()
            .expireAfterWrite(10, java.util.concurrent.TimeUnit.MINUTES)
            .build()
        
        fun startContextHistory(code: String) {
            contextHistory.put(code, Collections.synchronizedList(mutableListOf()))
        }
        
        fun logContextHistory(codes: Set<String?>) {
            log.info("Context history for $codes:\n${
                codes.flatMap { code ->
                    contextHistory.getIfPresent(code ?: "<none>") ?: emptyList()
                }
                    .sortedBy { it.ts }
                    .toTableString()
            }")
        }
        
    }
    
    
    fun recordSlice(slice: ContextSlice) {
        if (logAllSlices) {
            log.info("$slice")
        }
        contextHistory.getIfPresent(slice.codeBefore ?: "<none>")?.let {
            it.add(slice)
        }
        contextHistory.getIfPresent(slice.codeAfter ?: "<none>")?.let {
            it.add(slice)
        }
    }
    
    
    data class ContextSlice(
        val ts: Instant,
        val thread: String,
        val updateOrRestore: String,
        val codeBefore: String?,
        val codeAfter: String?,
        val subcode: String,
    )
    
    val activeExecs: MutableSet<Execution> =
        Collections.newSetFromMap(ConcurrentHashMap<Execution, Boolean>())
    
    val failed = OneTimeFlag()

    fun registerFailure() {
        failed.flagIfUnflagged()
        if (throwOnFail) {
            throw AssertionError("CoroContextDebugger failure")
        }
    }
    
    suspend inline fun <T> execute(crossinline block: suspend CoroutineScope.(exec: Execution) -> T): T {

        //        val code = Random.nextHex(8u)
        val code = seq.incrementAndGet()
            // Mathematicians hate This One Trick to add Leading Zeroes
            .plus(100000000).toString().takeLast(8)
        
        startContextHistory(code)
        val exec = Execution(this, code)
        activeExecs.add(exec)
        try {
            val tc = threadCode.get()
            checkThreadContext(tc, "before enter withContext")
            return withContext(exec) {
                checkThreadContext(exec.code, "after enter withContext")
                block(exec).also {
                    checkThreadContext(exec.code, "before exit withContext")
                }
            }.also {
                checkThreadContext(tc, "after exit withContext")
            }
        } finally {
            activeExecs.remove(exec)
        }
    }
    
    fun checkLeakedContexts() {
        if (activeExecs.isNotEmpty()) {
            registerFailure()
            log.warn("Leaked contexts: ${activeExecs.joinToString { it.code }}")
            logContextHistory(activeExecs.map { it.code }.toSet())
        }
        activeExecs.clear()
    }
    
    override fun close() {
        checkLeakedContexts()
    }
    
    class Execution(
        private val debugger: CoroContextDebugger,
        val code: String
    ) : ThreadContextElement<AutoCloseable> {
        
        object Key : CoroutineContext.Key<Execution>
        override val key get() = Key
        
        override fun updateThreadContext(context: CoroutineContext): AutoCloseable {
            val subcode = Random.nextHex(4u)
            val tc = threadCode.get()
//            log.info("${Thread.currentThread().desc()} context $tc -[$subcode]-> $code")
            val sliceStart = ContextSlice(Instant.now(), Thread.currentThread().desc(), "update", tc, code, subcode)
            debugger.recordSlice(sliceStart)
            threadCode.set(code)
            var closed: Throwable? = null
            return AutoCloseable {
                if (closed != null) {
                    log.warn("${Thread.currentThread().desc()} context $subcode being restored twice", closed)
                    // TODO also record
                    return@AutoCloseable
                }
                val now = Instant.now()
                val tc = threadCode.get()
                closed = Throwable("Closed at $now")
                val sliceEnd = ContextSlice(now, Thread.currentThread().desc(), "restore", code, tc, subcode)
//                log.info("${Thread.currentThread().desc()} context $tc <-[$subcode]- $code")
                if (tc == null) {
                    threadCode.remove()
                } else {
                    threadCode.set(tc)
                }
            }
        }
        
        override fun restoreThreadContext(context: CoroutineContext, oldState: AutoCloseable) {
            oldState.close()
        }
        
        suspend fun checkThreadContext(at: String) = debugger.checkThreadContext(code, at)
        
    }
    
    suspend fun checkThreadContext(code: String?, at: String) {
        require(code == null || code.length == 8) { "Invalid code: $code" }
        
        if (logAllSlices) {
            log.info("${Thread.currentThread().desc()} context $code check $at")
        }
        
        var logged = false
        val tc = threadCode.get()
        if (tc != code) {
            log.warn("${Thread.currentThread().desc()} should have context $code but has $tc $at")
            registerFailure()
            logContextHistory(setOf(code, tc))
            logged = true
        }
        
        // check coro context
        val coroCode = coroutineContext[Execution.Key]?.code
        if (coroCode != code) {
            log.warn("${Thread.currentThread().desc()} should have coro context $code but has $coroCode $at")
            registerFailure()
            if (!logged) {
                logContextHistory(setOf(code, coroCode))
            }
        }
    }
}

fun Thread.desc() = "Thread#${id}:'${name}'"
