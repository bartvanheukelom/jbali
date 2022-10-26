package org.jbali.exposed

import org.jetbrains.exposed.sql.*

/**
 * Represents the expression `COUNT(*)`
 */
object CountStar : org.jetbrains.exposed.sql.Function<Long>(LongColumnType()) {
    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        +"COUNT(*)"
    }
}

fun FieldSet.count(where: Op<Boolean> = Op.TRUE): Long =
    slice(listOf(CountStar)).select(where).single()[CountStar]
