package org.jbali.exposed

import org.jbali.util.SortOrder
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.compoundAnd

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

fun String.toSqlLiteral(): String =
    "'${replace("'", "''")}'"

fun compoundAnd(vararg ops: Op<Boolean>): Op<Boolean> =
    ops.toList().compoundAnd()

val SortOrder.exposed get() = when (this) {
    SortOrder.ASCENDING  -> org.jetbrains.exposed.sql.SortOrder.ASC
    SortOrder.DESCENDING -> org.jetbrains.exposed.sql.SortOrder.DESC
}
