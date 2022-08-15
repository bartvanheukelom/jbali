package org.jbali.coroutines

import kotlinx.coroutines.awaitCancellation


/**
 * For test code, simulate some task that _would_ eventually complete, but it just takes an
 * infinitely long time doing so. Can be used to test timeout code.
 * Simply an alias for [awaitCancellation], but with a more specific intent.
 */
suspend fun <T> delayIndefinitely(): T =
    awaitCancellation()
