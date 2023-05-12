package org.jbali.util

// https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md

/**
 * An alternative to `this@T` for referencing a context receiver,
 * and one that also works inside lambdas:
 *
 * ```
 * db.transaction {
 *   val tx = this@DBTransaction // unresolved reference, as of Kotlin 1.8.10
 *   val tx = contextual<DBTransaction>() // works
 * }
 * ```
 */
context(T)
inline fun <reified T> contextual(): T = this@T
