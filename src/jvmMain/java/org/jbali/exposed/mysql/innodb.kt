package org.jbali.exposed.mysql

import org.jbali.exposed.ExposedTxScope
import org.jbali.math.toLongBits
import org.jbali.sql.my.getCurrentInnoDBTrxInfo
import org.jbali.util.cast
import java.sql.Connection
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
context(ExposedTxScope)
fun innoDbTrxInfo(): Pair<Long, Instant>? =
    exposedTx.connection.connection.cast<Connection>().getCurrentInnoDBTrxInfo()
        ?.let { Pair(it.trxId.toLongBits(), it.trxStarted) }
