package org.jbali.exposed

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import org.jbali.kotser.BasicJson
import org.jbali.util.cast
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.IDateColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.JavaInstantColumnType
import org.jetbrains.exposed.sql.vendors.DatabaseDialect
import org.jetbrains.exposed.sql.vendors.MysqlDialect
import org.jetbrains.exposed.sql.vendors.currentDialect
import java.sql.ResultSet
import java.time.Instant
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
    get() = name.toSqlLiteral()

fun String.toSqlLiteral(): String =
    "'${replace("'", "''")}'"

fun <T : JsonElement> Table.myJson(name: String): Column<T> =
    registerColumn(name, JsonColumnType())

class JsonColumnType : ColumnType() {
    
    override fun sqlType(): String = "JSON"
    
    override fun valueFromDB(value: Any) =
        when (value) {
            is JsonElement -> value // TODO why is this one required
            is String -> BasicJson.parse(value)
            else -> throw IllegalArgumentException("Illegal value type for this column: $value")
        }
    
    override fun notNullValueToDB(value: Any) =
        when (value) {
            is JsonElement -> value
                .cast<JsonElement>()
                .let(BasicJson.plain::encodeToString)
            is String -> value // TODO why is this one required
            else -> throw IllegalArgumentException("Illegal value type for this column: $value")
        }
    
    override fun nonNullValueToString(value: Any): String =
        notNullValueToDB(value).toSqlLiteral()
    
}

/**
 * A timestamp column to store both a date and a time.
 *
 * @param name The column name
 */
fun Table.realTimestamp(name: String): Column<Instant> = registerColumn(name, InstantAsTimestampColumnType())

/**
 * Maps [Instant] to SQL `TIMESTAMP`, if available, as opposed to Exposed's default of mapping to `DATETIME`.
 */
class InstantAsTimestampColumnType : ColumnType(), IDateColumnType {
    override val hasTimePart: Boolean = true
    override fun sqlType(): String = currentDialect.timestampType()
    
    private val original = JavaInstantColumnType()
    override fun nonNullValueToString(value: Any): String    = original.nonNullValueToString(value)
    override fun valueFromDB(value: Any): Instant            = original.valueFromDB(value)
    override fun readObject(rs: ResultSet, index: Int): Any? = original.readObject(rs, index)
    override fun notNullValueToDB(value: Any): Any           = original.notNullValueToDB(value)
}

fun DatabaseDialect.timestampType(): String =
    (this as? MysqlDialect)?.let {
        if (it.isFractionDateTimeSupported()) "TIMESTAMP(6)" else "TIMESTAMP"
    } ?: "DATETIME"
