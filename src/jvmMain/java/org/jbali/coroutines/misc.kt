package org.jbali.coroutines

import kotlinx.coroutines.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
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

@ExperimentalTime
suspend fun <T> Deferred<T>.awaitFor(timeout: Duration) =
    withTimeout(timeout) { awaitOrCancel() }
