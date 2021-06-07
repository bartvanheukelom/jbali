package org.jbali.exposed

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import org.jbali.kotser.BasicJson
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import kotlin.reflect.KClass

inline fun <reified T : Enum<T>> Table.myEnum(name: String): Column<T> =
    myEnum(name, T::class)

fun <T : Enum<T>> Table.myEnum(name: String, klass: KClass<T>): Column<T> {
    
    val vals = klass.java.enumConstants!!
    val byName = vals.associateBy { it.name }
    
    return customEnumeration(
        name = name,
        sql = "ENUM(${vals.joinToString {
            it.sqlLiteral
        }})",
        fromDb = { byName.getValue(it as String) },
        toDb = { it.name }
    )
}

val <E : Enum<E>> E.sqlLiteral: String
    get() = "'" + name.replace("'", "''") + "'"

fun <T : JsonElement> Table.myJson(name: String): Column<T> =
    registerColumn(name, JsonColumnType())

class JsonColumnType : ColumnType() {
    override fun sqlType(): String = "JSON"
    
    override fun valueFromDB(value: Any) =
        BasicJson.parse(value as String)
    
    override fun notNullValueToDB(value: Any) =
        BasicJson.plain.encodeToString(value as JsonElement)
    
}
