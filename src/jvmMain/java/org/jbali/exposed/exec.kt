package org.jbali.exposed

import org.intellij.lang.annotations.Language
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi

/**
 * Executes the given SQL statement, which must be an SQL Data Manipulation Language (DML) statement, such as INSERT, UPDATE or DELETE; or an SQL statement that returns nothing, such as a DDL statement.
 * @return The row count for SQL Data Manipulation Language (DML) statements or 0 for SQL statements that return nothing.
 */
fun Transaction.execUpdate(
    @Language("sql") stmt: String,
): Int =
    exec(object : Statement<Int>(statementTypeOf(stmt), emptyList()) {
        override fun arguments() = emptyList<Nothing>()
        override fun prepareSQL(transaction: Transaction) = stmt
        override fun PreparedStatementApi.executeInternal(transaction: Transaction): Int =
            executeUpdate()
    })!!

/**
 * Determine the [StatementType] of the given SQL statement by looking at its first word.
 */
fun statementTypeOf(@Language("sql") stmt: String): StatementType =
    StatementType.values().single { stmt.trim().startsWith(it.name, true) }
