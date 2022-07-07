package org.jbali.coroutines

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.concurrent.thread


private val log = LoggerFactory.getLogger("org.jbali.coroutines.loops")

fun <T> startSingleThreadCoroutine(
    name: String,
    body: suspend CoroutineScope.() -> T,
): Deferred<T> {
//    val log = logger("com.redwinx.singleThreadCoroutine[$name]")
    
    val resultReturner = CompletableDeferred<T>()
    val jobThread = thread(
        name = name,
    ) {
        try {
            runBlocking {
                body()
            }
                .let { resultReturner.complete(it) }
        } catch (ce: InterruptedException) {
//            log.info("runBlocking threw $ce")
            if (resultReturner.isActive) {
//                log.info("resultReturner still active, going to cancel")
                resultReturner.cancel()
            }
        } catch (e: Throwable) {
//            log.warn("Completing with exception: $e")
            resultReturner.completeExceptionally(e)
        }
    }
    
    resultReturner.invokeOnCompletion { cause ->
//        log.info("resultReturner completed: $cause")
        if (cause is CancellationException) {
//            log.info("Going to interrupt thread")
            // if runBlocking already returned, this does nothing
            jobThread.interrupt()
        }
    }
    
    return resultReturner
}

fun startSingleThreadDelayLoop(
    name: String,
    delayMs: Long,
    errorLogger: (Throwable) -> Unit,
    body: suspend () -> Unit,
): Job =
    startSingleThreadCoroutine(
        name = name
    ) {
        runDelayLoop(
            delayMs = delayMs,
            errorLogger = errorLogger,
            body = body,
        )
    }

suspend fun runDelayLoop(
    delayMs: Long,
    errorLogger: (Throwable) -> Unit,
    body: suspend () -> Unit,
) {
    while (true) {
        delay(delayMs)
        try {
            body()
//        } catch (ce: CancellationException) {
//            throw ce
        } catch (e: Throwable) {
            try {
                errorLogger(e)
            } catch (le: Throwable) {
                try {
                    log.warn("runDelayLoop errorLogger failed to log a ${e.javaClass.canonicalName}", le)
                } catch (lle: Throwable) {
                    try {
                        e.printStackTrace()
                        le.printStackTrace()
                        lle.printStackTrace()
                    } catch (llle: Throwable) {
                        // dude...
                    }
                }
            }
        }
    }
}
