package org.jbali.exposed

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import kotlin.reflect.KClass

inline fun <reified T : Enum<T>> Table.enumerationAsMySQLEnum(name: String): Column<T> =
    enumerationAsMySQLEnum(name, T::class)

fun <T : Enum<T>> Table.enumerationAsMySQLEnum(name: String, klass: KClass<T>): Column<T> {
    
    val vals = klass.java.enumConstants!!
    val byName = vals.associateBy { it.name }
    
    return customEnumeration(
        name = name,
        sql = "ENUM(${vals.joinToString {
            "'${it.name.replace("'", "''")}'"
        }})",
        fromDb = { byName.getValue(it as String) },
        toDb = { it.name }
    )
}
