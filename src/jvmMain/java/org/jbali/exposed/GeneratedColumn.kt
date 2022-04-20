package org.jbali.exposed

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager

class GeneratedColumn<T>(
    /** Table where the columns is declared. */
    val table: Table,
    /** Name of the column. */
    val name: String,
    /** Data type of the column. */
    override val columnType: IColumnType
) : ExpressionWithColumnType<T>() {
    
    /** Appends the SQL representation of this column to the specified [queryBuilder]. */
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        TransactionManager.current().fullIdentity(queryBuilder)
    }
    
    private fun Transaction.fullIdentity(queryBuilder: QueryBuilder) = queryBuilder {
        if (table is Alias<*>) {
            append(db.identifierManager.quoteIfNecessary(table.alias))
        } else {
            append(db.identifierManager.quoteIfNecessary(table.tableName.inProperCaseNIC()))
        }
        append('.')
        append(identity())
    }
    
    private fun Transaction.identity(): String = db.identifierManager.quoteIdentifierWhenWrongCaseOrNecessary(name)
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeneratedColumn<*>) return false
        
        if (table != other.table) return false
        if (name != other.name) return false
        if (columnType != other.columnType) return false
        
        return true
    }
    
    override fun hashCode(): Int = table.hashCode() * 31 + name.hashCode()
    
    override fun toString(): String = "${table.javaClass.name}.$name"
}

// NIC = non-internal copy
fun String.inProperCaseNIC(): String =
    TransactionManager.currentOrNull()?.db?.identifierManager?.inProperCase(this@inProperCaseNIC) ?: this
