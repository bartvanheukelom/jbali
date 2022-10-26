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
