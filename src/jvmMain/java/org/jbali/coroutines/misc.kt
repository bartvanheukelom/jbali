package org.jbali.coroutines

import kotlinx.coroutines.*
import org.jbali.util.Box
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Invoke [block] within [withTimeout] if [timeout] is not `null`, or directly if it is.
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalTime
suspend fun <T> withOptionalTimeout(timeout: Duration? = null, block: suspend () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return if (timeout != null) {
        withTimeout(timeout) {
            block()
        }
    } else {
        block()
    }
}


/**
 * Calls [Deferred.await]. If an exception is thrown by await, e.g. because it's cancelled by a
 * containing [withTimeout], the deferred is cancelled (if it needs to be) with [Job.cancel],
 * before rethrowing the exception.
 *
 * Any exception throw during the cancellation is added to the supressed of the original exception.
 */
suspend fun <T> Deferred<T>.awaitOrCancel() =
    try {
        await()
    } catch (ce: Throwable) {
        try {
            if (!isCancelled) {
                try {
                    cancel()
                } catch (jc: CancellationException) {
                    // alrighty then
                }
            }
        } catch (e: Throwable) {
            ce.addSuppressed(e)
        }
        throw ce
    }

/**
 * Calls [Deferred.awaitOrCancel] within [withTimeout]. That is, if the Deferred is not completed
 * within [timeout], the Deferred is cancelled, and a [TimeoutCancellationException] is thrown from
 * this function.
 */
@ExperimentalTime
suspend fun <T> Deferred<T>.awaitFor(timeout: Duration) =
    withTimeout(timeout) { awaitOrCancel() }

/**
 * Calls [Deferred.await] within [withTimeoutOrNull].
 * If the Deferred is not completed within [timeout], this function will return `null`,
 * but the Deferred will _not_ be cancelled.
 * If the Deferred is completed within [timeout], the result is wrapped in a [Box].
 */
@ExperimentalTime
suspend fun <T> Deferred<T>.awaitOrNull(timeout: Duration): Box<T>? =
    withTimeoutOrNull(timeout) { Box(await()) }


/**
 * Calls [runBlocking] with the given [context] and [block].
 * If the thread is interrupted while running the block, throws an exception, like [runBlocking] does,
 * but unlike that function, the thrown exception will specify during what operation the coroutine was cancelled,
 * if possible. The thrown exception does not have to be [InterruptedException], but will have it as a cause.
 */
fun <T> runBlockingInterruptable(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T
): T {
    var innerException: Throwable? = null
    return try {
        runBlocking(context) {
            try {
                block()
            } catch (e: Throwable) {
                innerException = e
                throw e
            }
        }
    } catch (e: InterruptedException) {
        val ie = innerException
        when {
            
            ie == null -> throw e
            
            // TODO move hasCause to jbali
            ie.cause === e
                    || ie.cause?.cause === e
                    || ie.cause?.cause?.cause === e
                    || ie.cause?.cause?.cause?.cause === e
                    || ie.cause?.cause?.cause?.cause?.cause === e
                -> throw ie
            
            else -> {
                e.addSuppressed(ie)
                throw e
            }
        }
    }
}
