package org.jbali.exposed

import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select

fun <T : Comparable<T>> IdTable<T>.select(id: T): Query =
    let { tbl ->
        select { tbl.id eq id }
    }

fun <T : Comparable<T>> IdTable<T>.selectSingle(id: T): ResultRow =
    select(id).single()

fun <T : Comparable<T>> IdTable<T>.selectSingleOrNull(id: T): ResultRow? =
    select(id).singleOrNull()


//@JvmName("setWithEntityIdValue")
//operator fun <S : Comparable<S>, ID : EntityID<S>, E : S?>
//        UpdateBuilder<*>.set(column: Column<ID>, value: E) {
//    this[column] = EntityID(value, column.referee<EntityID<E>>().table as IdTable<E>)
//
//    require(!values.containsKey(column)) { "$column is already initialized" }
//    values[column] = value
//}