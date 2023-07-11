@file:OptIn(ExperimentalContracts::class)

package org.jbali.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Equivalent to:
 *
 * ```
 * this
 *     .let(block) // 1
 *     .let(block) // 2
 *     // ...
 *     .let(block) // times - 1
 *     .let(block) // times
 * ```
 */
inline fun <T> T.letRepeat(times: Int, block: (T) -> T): T {
    contract {
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    var result = this
    repeat(times) {
        result = block(result)
    }
    return result
}
