package org.jbali.exposed

import org.jetbrains.exposed.sql.*

typealias BoolOp = Op<Boolean>

/**
 * [BoolOp] that returns true if all of the given [ops] return true.
 */
fun compoundAnd(vararg ops: BoolOp): BoolOp =
    ops.toList()
        .filter { it != Op.TRUE }
        .let {
            if (it.isEmpty()) Op.TRUE
            else it.compoundAnd()
        }

/**
 * [BoolOp] that returns true if any of the given [ops] return true.
 */
fun compoundOr(vararg ops: BoolOp): BoolOp =
    ops.toList()
        .filter { it != Op.FALSE }
        .let {
            if (it.isEmpty()) Op.FALSE
            else it.compoundOr()
        }


fun SqlExpressionBuilder.all(vararg ops: BoolOp) = compoundAnd(*ops)
fun SqlExpressionBuilder.any(vararg ops: BoolOp) = compoundOr(*ops)


class SqlExpr<T>(private val sqlText: String) : Expression<T>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append(sqlText)
    }
}

class SqlOp<T>(private val sqlText: String) : Op<T>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append(sqlText)
    }
}

fun sqlBoolOp(sqlText: String) = SqlOp<Boolean>(sqlText)
