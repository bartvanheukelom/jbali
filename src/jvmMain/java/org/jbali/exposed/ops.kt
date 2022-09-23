package org.jbali.exposed

import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.compoundAnd

fun compoundAnd(vararg ops: Op<Boolean>): Op<Boolean> =
    ops.toList().filter { it != Op.TRUE }.compoundAnd()

class SqlOp<T>(private val sqlText: String) : Op<T>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append(sqlText)
    }
}

fun sqlBoolOp(sqlText: String) = SqlOp<Boolean>(sqlText)
