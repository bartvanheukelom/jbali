package org.jbali.exposed

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import org.jbali.kotser.BasicJson
import org.jbali.util.cast
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.JavaInstantColumnType
import org.jetbrains.exposed.sql.vendors.DatabaseDialect
import org.jetbrains.exposed.sql.vendors.MysqlDialect
import org.jetbrains.exposed.sql.vendors.currentDialect
import java.sql.ResultSet
import java.time.Instant
import kotlin.reflect.KClass


// TODO this style results in api combinatory explosion. replace with:
//      - some kind of builder
//      - a column modifier (could use replaceColumn like nullable() does, but why? the column itself is mutable...)

/**
 * A [Table.reference] that cascades updates and deletes.
 */
fun <T : Comparable<T>> Table.parentReference(name: String, foreign: IdTable<T>, fkName: String? = null) =
    reference(
        name = name, foreign = foreign, fkName = fkName,
        onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE,
    )
/**
 * A nullable [Table.reference] that cascades updates and sets null on delete.
 */
fun <T : Comparable<T>> Table.weakReference(name: String, foreign: IdTable<T>, fkName: String? = null) =
    reference(
        name = name, foreign = foreign, fkName = fkName,
        onDelete = ReferenceOption.SET_NULL, onUpdate = ReferenceOption.CASCADE,
    ).nullable()





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
fun Table.realTimestamp(name: String, fsp: Int? = null): Column<Instant> =
    registerColumn(name, InstantAsTimestampColumnType())

/**
 * Maps [Instant] to SQL `TIMESTAMP`, as opposed to Exposed's default of mapping to `DATETIME`.
 *
 * @param fsp Fractional second precision.
 *            If `null`, defaults to the maximum precision supported by the database.
 *            If a number is given, an error is thrown if it's not supported.
 */
class InstantAsTimestampColumnType(
    val fsp: Int? = null
) : ColumnType(), IDateColumnType {
    override val hasTimePart: Boolean = true
    override fun sqlType(): String = currentDialect.timestampType(fsp)
    
    private val original = JavaInstantColumnType()
    override fun nonNullValueToString(value: Any): String    = original.nonNullValueToString(value)
    override fun valueFromDB(value: Any): Instant            = original.valueFromDB(value)
    override fun readObject(rs: ResultSet, index: Int): Any? = original.readObject(rs, index)
    override fun notNullValueToDB(value: Any): Any           = original.notNullValueToDB(value)
}

fun DatabaseDialect.timestampType(fsp: Int? = null): String {
    require(fsp in 0 .. 6)
    return when {
        this !is MysqlDialect -> throw IllegalArgumentException("Timestamp support unknown for this database")
        isFractionDateTimeSupported() -> "TIMESTAMP(${fsp ?: 6})"
        fsp != null && fsp > 0 -> throw IllegalArgumentException("fsp $fsp not supported by this version of MySQL")
        else -> "TIMESTAMP"
    }
}
