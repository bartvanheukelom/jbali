package org.jbali.exposed

import org.jbali.util.SortOrder
import org.jetbrains.exposed.sql.*


val <E : Enum<E>> E.sqlLiteral: String
    get() = name.toSqlLiteral()

private val tct = TextColumnType()

fun String.toSqlLiteral(): String =
//    "'${replace("'", "''")}'" - doesn't handle newlines
    tct.nonNullValueToString(this)

val SortOrder.exposed get() = when (this) {
    SortOrder.ASCENDING  -> org.jetbrains.exposed.sql.SortOrder.ASC
    SortOrder.DESCENDING -> org.jetbrains.exposed.sql.SortOrder.DESC
}


// TODO Slice should extend ColumnSet
fun FieldSet.slice(columns: List<Expression<*>>) =
    when (this) {
        is ColumnSet -> slice(columns)
        is Slice -> source.slice(columns)
        else -> throw IllegalArgumentException("Cannot slice $this")
    }

/**
 * Group by the given columns and also order by them.
 * The latter often happens implicitly due to the grouping implementation,
 * but is not actually guaranteed by the SQL standard.
 */
fun <Q : Query> Q.groupAndOrderBy(vararg columns: Expression<*>): Q =
    apply {
        groupBy(*columns)
        orderBy(*columns)
    }

fun <Q : AbstractQuery<Q>> Q.orderBy(vararg order: Expression<*>): Q =
    orderBy(*order.map { it to org.jetbrains.exposed.sql.SortOrder.ASC }.toTypedArray())
