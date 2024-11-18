package org.jbali.exposed

import org.intellij.lang.annotations.Language
import org.jbali.jdbc.map
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.Statement
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import java.sql.ResultSet

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

/**
 * Execute the given query, which should be a SELECT or some other statement that returns a result set,
 * and map the result rows to a list of [T].
 */
fun <T> Transaction.execAndMap(
    @Language("sql") stmt: String,
    args: Iterable<Pair<IColumnType, Any?>> = emptyList(),
    explicitStatementType: StatementType? = null,
    transform: (ResultSet) -> T
): List<T> =
    exec(stmt, args, explicitStatementType) { rs ->
        rs.map(transform)
    } ?: error("No result set")
