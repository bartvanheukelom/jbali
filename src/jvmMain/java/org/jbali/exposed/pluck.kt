package org.jbali.exposed

import org.jbali.collect.map
import org.jbali.collect.singleIfAny
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*

fun <T> ColumnSet.pluck(expr: Expression<T>) =
    PluckedField(expr, slice(expr))

open class PluckedField<T>(
    val expr: Expression<T>,
    val slice: FieldSet,
) {
    inline fun select(where: SqlExpressionBuilder.() -> Op<Boolean>) =
        selectWhere(SqlExpressionBuilder.where())
    fun selectWhere(where: Op<Boolean>) =
        PluckingQuery(
            expr = expr,
            query = slice.select(where),
        )
}


fun <T : Table, E> T.pluck(expr: Expression<E>) =
    PluckedTableColumn(this, expr)

// would prefer T.() -> Expression, but this is consistent with select()
fun <T : Table, E> T.pluck(plucker: (T) -> Expression<E>) =
    pluck(plucker(this))

// this unfortunately won't work because Column doesn't preserve Table type
//fun <T : Table, E> Column<E>.pluck() =
//    table.pluck(this)

open class PluckedTableColumn<T : Table, E>(
    val table: T,
    expr: Expression<E>,
) : PluckedField<E>(expr, table.slice(expr)) {
    inline fun select(where: SqlExpressionBuilder.(T) -> Op<Boolean>) =
        selectWhere(SqlExpressionBuilder.where(table))
}


fun <I : Comparable<I>, T : IdTable<I>, E> T.pluck(plucker: (T) -> Expression<E>) =
    PluckedIdTableColumn(this, plucker(this))

class PluckedIdTableColumn<I : Comparable<I>, T : IdTable<I>, E>(
    table: T,
    expr: Expression<E>,
) : PluckedTableColumn<T, E>(table, expr) {
    
    // TODO seems useless, id must always return 0 or 1
    fun select(id: I): PluckingQuery<E> =
        select { -> table.id eq id }
    
    fun selectSingle(id: I): E =
        select(id).single()
}

fun <I : Comparable<I>, T : IdTable<I>, E> T.get(id: I, plucker: (T) -> Expression<E>): E =
    pluck(plucker).selectSingle(id)
fun <I : Comparable<I>, T : IdTable<I>, E> T.getOrNull(id: I, plucker: (T) -> Expression<E>): E? =
    pluck(plucker).select(id).singleIfAny()


class PluckingQuery<T>(
    val expr: Expression<T>,
    val query: Query,
) :
    SizedIterable<T>
{
    override fun copy(): SizedIterable<T> = TODO()
    
    override fun count(): Long = query.count()
    override fun empty(): Boolean = query.empty()
    override fun limit(n: Int, offset: Long): PluckingQuery<T> =
        apply { query.limit(n, offset) }
    
    override fun orderBy(vararg order: Pair<Expression<*>, SortOrder>): PluckingQuery<T> =
        apply { query.orderBy(*order) }
    
    override fun iterator(): Iterator<T> =
        query.iterator().map { it[expr] }
}