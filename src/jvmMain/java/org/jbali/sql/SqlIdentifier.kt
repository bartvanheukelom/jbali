package org.jbali.sql

@JvmInline value class SqlIdentifier private constructor(
    /**
     * The identifier in a format suitable for embedding into an SQL statement as-is,
     * so already escaped and backquoted if necessary.
     */
    val sql: String
) {
    companion object {
        fun asIs(sql: String) = SqlIdentifier(sql)
    }
}
