package org.jbali.exposed

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.StringColumnType
import org.jetbrains.exposed.sql.Table
import kotlin.reflect.KClass

inline fun <reified T : Enum<T>> Table.myEnum(name: String): Column<T> =
    myEnum(name, T::class)

fun <T : Enum<T>> Table.myEnum(name: String, klass: KClass<T>): Column<T> =
    registerColumn(
        name = name,
        type = MyEnumColumnType(klass),
    )

class MyEnumColumnType<T : Enum<T>>(
    val enumClass: KClass<T>,
) : StringColumnType() {
    
    private val vals = enumClass.java.enumConstants!!
    private val byName = vals.associateBy { it.name }
    
    override fun sqlType(): String =
        "ENUM(${vals.joinToString { it.sqlLiteral }})"
    
    // TODO add function to validate against actual column definition
    
    @Suppress("UNCHECKED_CAST")
    override fun valueFromDB(value: Any): T =
        when {
            enumClass.isInstance(value) -> value as T
            value is String -> byName.getValue(value)
            else -> throw IllegalArgumentException("Illegal value type for this column: $value")
        }
    
    @Suppress("UNCHECKED_CAST")
    override fun notNullValueToDB(value: Any): String =
        when {
            enumClass.isInstance(value) -> (value as T).name
            value is String -> value
            else -> throw IllegalArgumentException("Illegal value type for this column: $value")
        }
    
    override fun nonNullValueToString(value: Any): String =
        notNullValueToDB(value).toSqlLiteral()
}