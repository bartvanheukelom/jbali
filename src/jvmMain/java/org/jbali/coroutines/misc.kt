package org.jbali.coroutines

import kotlinx.coroutines.withTimeout
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
