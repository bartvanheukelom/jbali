package org.jbali.exposed.mysql

import org.jbali.exposed.ExposedTxScope
import org.jbali.util.boxed
import java.time.Instant

/**
 * Return the InnoDB transaction ID and start time of the current transaction, or null if not found for some reason.
 *
 * Possible reasons include:
 *
 * - not running in a transaction
 * - running in a read-only transaction (which each transaction starts as)
 * - information_schema not yet updated with the transaction (!)
 *
 * WARNING: that last item may also present itself as a present but outdated transaction ID.
 * Use the start time to check for this.
 *
 * Besides returning `null`, this function can also throw, e.g. if
 * the user does not have the required permissions to access the
 * information_schema, or the database is not MySQL.
 */
// TODO to jbali
context(ExposedTxScope)
fun innoDbTrxInfo(): Pair<Long, Instant>? =
    exposedTx.exec("""
        SELECT trx_id,
               UNIX_TIMESTAMP(CONVERT_TZ(trx_started, @@GLOBAL.time_zone, @@SESSION.time_zone)) as trx_started_unix
        FROM information_schema.innodb_trx
        WHERE trx_mysql_thread_id = connection_id()
    """.trimIndent()
    ) { rs ->
        if (rs.next()) {
            val id = rs.getLong(1)
            val started = rs.getLong(2).let { Instant.ofEpochSecond(it) }
            Pair(id, started)
        } else {
            null
        }.boxed() // exposed doesn't allow returning a bare null here
    }?.contents
