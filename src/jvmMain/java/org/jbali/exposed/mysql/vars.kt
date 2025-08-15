package org.jbali.exposed.mysql

import org.jbali.exposed.ExposedTxScope
import org.jbali.exposed.execUpdate
import org.jbali.random.nextHex
import org.jbali.sql.SqlIdentifier
import java.time.Duration
import kotlin.random.Random


/**
 * Set system variable [name] to [value] in the current session, for the duration of the block,
 * restoring the previous value afterwards.
 *
 * *Warning*: If the variable had no session value before, i.e. its value came from the global
 * scope, then afterwards it will have a session value equal to that global value, and any
 * changes to the global value will no longer affect the session value for the rest of the session.
 */
context(ExposedTxScope)
fun <T> withSystemVar(name: SqlIdentifier, value: Int, block: () -> T): T {
    val backupName = "savedVar_${Random.nextHex(8u)}" // TODO instead of savedVar, use name, but safely
    exposedTx.execUpdate("SET @$backupName = @@${name.sql}")
    return try {
        exposedTx.execUpdate("SET @@${name.sql} = $value")
        block()
    } finally {
        exposedTx.execUpdate("SET @@${name.sql} = @$backupName")
    }
}

context(ExposedTxScope)
fun <T> withMaxExecutionTime(time: Duration, block: () -> T) =
    withSystemVar(SqlIdentifier.asIs("max_execution_time"), time.toMillis().toInt(), block)

context(ExposedTxScope)
fun <T> withInnoDBLockWaitTimeout(time: Duration, block: () -> T) =
    withSystemVar(SqlIdentifier.asIs("innodb_lock_wait_timeout"), time.toSeconds().toInt(), block)
