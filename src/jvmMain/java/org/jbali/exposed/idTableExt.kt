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
