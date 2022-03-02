package org.jbali.exposed

import org.jbali.util.SortOrder
import org.jetbrains.exposed.sql.*

/**
 * Represents the expression `COUNT(*)`
 */
object CountStar : org.jetbrains.exposed.sql.Function<Long>(LongColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        +"COUNT(*)"
    }
}

val <E : Enum<E>> E.sqlLiteral: String
    get() = name.toSqlLiteral()

private val tct = TextColumnType()

fun String.toSqlLiteral(): String =
//    "'${replace("'", "''")}'" - doesn't handle newlines
    tct.nonNullValueToString(this)

fun compoundAnd(vararg ops: Op<Boolean>): Op<Boolean> =
    ops.toList().filter { it != Op.TRUE }.compoundAnd()

val SortOrder.exposed get() = when (this) {
    SortOrder.ASCENDING  -> org.jetbrains.exposed.sql.SortOrder.ASC
    SortOrder.DESCENDING -> org.jetbrains.exposed.sql.SortOrder.DESC
}
