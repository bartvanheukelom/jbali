package org.jbali.sql.my

import org.jbali.math.toLongBits
import org.jbali.math.toLongExact
import org.jbali.math.toULongBits
import java.sql.Connection
import java.time.Instant

/*

https://dev.mysql.com/doc/refman/8.4/en/information-schema-innodb-trx-table.html

| Field | Type | Null | Key | Default | Extra |
| :--- | :--- | :--- | :--- | :--- | :--- |
| trx\_id | bigint unsigned | NO |  |  |  |
| trx\_state | varchar\(13\) | NO |  |  |  |
| trx\_started | datetime | NO |  |  |  |
| trx\_requested\_lock\_id | varchar\(105\) | YES |  |  |  |
| trx\_wait\_started | datetime | YES |  |  |  |
| trx\_weight | bigint unsigned | NO |  |  |  |
| trx\_mysql\_thread\_id | bigint unsigned | NO |  |  |  |
| trx\_query | varchar\(1024\) | YES |  |  |  |
| trx\_operation\_state | varchar\(64\) | YES |  |  |  |
| trx\_tables\_in\_use | bigint unsigned | NO |  |  |  |
| trx\_tables\_locked | bigint unsigned | NO |  |  |  |
| trx\_lock\_structs | bigint unsigned | NO |  |  |  |
| trx\_lock\_memory\_bytes | bigint unsigned | NO |  |  |  |
| trx\_rows\_locked | bigint unsigned | NO |  |  |  |
| trx\_rows\_modified | bigint unsigned | NO |  |  |  |
| trx\_concurrency\_tickets | bigint unsigned | NO |  |  |  |
| trx\_isolation\_level | varchar\(16\) | NO |  |  |  |
| trx\_unique\_checks | int | NO |  |  |  |
| trx\_foreign\_key\_checks | int | NO |  |  |  |
| trx\_last\_foreign\_key\_error | varchar\(256\) | YES |  |  |  |
| trx\_adaptive\_hash\_latched | int | NO |  |  |  |
| trx\_adaptive\_hash\_timeout | bigint unsigned | NO |  |  |  |
| trx\_is\_read\_only | int | NO |  |  |  |
| trx\_autocommit\_non\_locking | int | NO |  |  |  |
| trx\_schedule\_weight | bigint unsigned | YES |  |  |  |
 */

data class InnoDBTrxInfo(
    val trxId: ULong,
    val trxState: InnoDBTrxState,
    val trxStarted: Instant,
//    val trxRequestedLockId: String?,
//    val trxWaitStarted: String?, // datetime
//    val trxWeight: ULong,
//    val trxMysqlThreadId: ULong,
//    val trxQuery: String?,
//    val trxOperationState: String?,
//    val trxTablesInUse: ULong,
//    val trxTablesLocked: ULong,
//    val trxLockStructs: ULong,
//    val trxLockMemoryBytes: ULong,
//    val trxRowsLocked: ULong,
//    val trxRowsModified: ULong,
//    val trxConcurrencyTickets: ULong,
//    val trxIsolationLevel: String,
//    val trxUniqueChecks: Int,
//    val trxForeignKeyChecks: Int,
//    val trxLastForeignKeyError: String?,
//    val trxAdaptiveHashLatched: Int,
//    val trxAdaptiveHashTimeout: ULong,
//    val trxIsReadOnly: Int,
//    val trxAutocommitNonLocking: Int,
//    val trxScheduleWeight: ULong?,
)

enum class InnoDBTrxState(val dbName: String) {
    Running("RUNNING"),
    LockWait("LOCK WAIT"),
    RollingBack("ROLLING BACK"),
    Committing("COMMITTING"),
}


/**
 * Retrieves the InnoDB transaction info for the given MySQL thread ID, or for the current connection's thread if null.
 */
fun Connection.getCurrentInnoDBTrxInfo(
    threadId: ULong? = null,
): InnoDBTrxInfo? {
   prepareStatement("""
       SELECT trx_id, trx_state,
           UNIX_TIMESTAMP(CONVERT_TZ(trx_started, @@GLOBAL.time_zone, @@SESSION.time_zone)) as trx_started_unix
       FROM information_schema.innodb_trx
       WHERE trx_mysql_thread_id = ${threadId?.let { "?" } ?: "CONNECTION_ID()"}
   """.trimIndent())
       .use { ps ->
           if (threadId != null) {
               ps.setLong(1, threadId.toLongBits())
           }
           ps.executeQuery().use { rs ->
               return if (rs.next()) {
                   InnoDBTrxInfo(
                       trxId = rs.getLong("trx_id").toULongBits(),
                       trxState = rs.getString("trx_state")
                           .let { v -> InnoDBTrxState.entries.single { it.dbName == v } },
                       trxStarted = Instant.ofEpochSecond(rs.getLong("trx_started_unix")),
                   )
               } else {
                   null
               }
           }
       }
}
