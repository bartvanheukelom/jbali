package org.jbali.exposed

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import org.jbali.kotser.DefaultJson
import org.jbali.kotser.JsonSerializer
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import kotlin.reflect.KClass

inline fun <reified T : Any> Table.myObjectAsJson(name: String): Column<T> =
    registerColumn(name, ObjectAsJsonColumnType(T::class, serializer<T>()))

class ObjectAsJsonColumnType<T : Any>(
    private val klass: KClass<T>,
    private val serializer: KSerializer<T>,
    private val jsonFormat: Json = DefaultJson.plainOmitDefaults,
) : ColumnType() {
    
    private val js = JsonSerializer(serializer, jsonFormat)
    
    override fun sqlType(): String = "JSON"
    
    override fun valueFromDB(value: Any): T =
        when {
            klass.isInstance(value) ->
                // possibly of wrong type, but who calls this path anyway?
                @Suppress("UNCHECKED_CAST")
                value as T
            
            value is JsonElement -> js.decodeFromElement(value)
            
            value is String -> js.parseJsonString(value)
            
            else -> throw IllegalArgumentException("Illegal value type for this column: $value")
        }
    
    override fun notNullValueToDB(value: Any): String =
        when {
            klass.isInstance(value) ->
                // possibly of wrong type, but what can we do?
                @Suppress("UNCHECKED_CAST")
                js.stringify(value as T).string
            
//            value is JsonElement ->
//                .cast<JsonElement>()
//                .let(BasicJson.plain::encodeToString)
            
            value is String -> value // TODO why is this one required
            
            else -> throw IllegalArgumentException("Illegal value type for this column: $value")
        }
    
    override fun nonNullValueToString(value: Any): String =
        notNullValueToDB(value).toSqlLiteral()
    
}