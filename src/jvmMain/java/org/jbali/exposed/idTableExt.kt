package org.jbali.exposed

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select

// TODO seems useless, id must always return 0 or 1
fun <T : Comparable<T>> IdTable<T>.select(id: T): Query =
    let { tbl ->
        select { tbl.id eq id }
    }

fun <T : Comparable<T>> IdTable<T>.selectSingle(id: T): ResultRow =
    select(id).single()

fun <T : Comparable<T>> IdTable<T>.selectSingleOrNull(id: T): ResultRow? =
    select(id).singleOrNull()

fun <T : Comparable<T>> IdTable<T>.deleteSingle(id: T): Boolean =
    let { tbl ->
        deleteWhere {
            tbl.id eq id
        } > 0
    }


//@JvmName("setWithEntityIdValue")
//operator fun <S : Comparable<S>, ID : EntityID<S>, E : S?>
//        UpdateBuilder<*>.set(column: Column<ID>, value: E) {
//    this[column] = EntityID(value, column.referee<EntityID<E>>().table as IdTable<E>)
//
//    require(!values.containsKey(column)) { "$column is already initialized" }
//    values[column] = value
//}

open class VarcharIdTable(name: String = "", columnName: String = "id", length: Int = 255) : IdTable<String>(name) {
    override val id: Column<EntityID<String>> = varchar(columnName, length).entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}
