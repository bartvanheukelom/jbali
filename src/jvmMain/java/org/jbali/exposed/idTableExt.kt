package org.jbali.exposed

import org.jbali.util.cast
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateBuilder

fun <T : Comparable<T>> IdTable<T>.idValue(id: T) = EntityID(id, this)

fun <T : Comparable<T>, I : IdTable<T>> I.insert(idVal: T, body: I.(InsertStatement<Number>) -> Unit) =
    insert {
        it[id] = idValue(idVal)
        body(it)
    }

fun <T : Comparable<T>, I : IdTable<T>> I.insertIgnore(idVal: T, body: I.(UpdateBuilder<*>) -> Unit) =
    insertIgnore {
        it[id] = idValue(idVal)
        body(it)
    }

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

@OptIn(ExperimentalUnsignedTypes::class)
open class ULongIdTable(name: String = "", columnName: String = "id") : IdTable<ULong>(name) {
    override val id: Column<EntityID<ULong>> = ulong(columnName).entityId()
    override val primaryKey by lazy { super.primaryKey ?: PrimaryKey(id) }
}

val <T : Comparable<T>> Column<EntityID<T>>.idColumn: Column<T>
    get() = columnType.cast<EntityIDColumnType<T>>().idColumn

infix fun <T : Comparable<T>> Column<EntityID<T>>.eq(other: Expression<T>) =
    idColumn eq other
