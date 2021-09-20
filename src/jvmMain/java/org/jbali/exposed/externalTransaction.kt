package org.jbali.exposed

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.exposedLogger
import org.jetbrains.exposed.sql.statements.jdbc.JdbcConnectionImpl
import org.jetbrains.exposed.sql.transactions.TransactionInterface
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

fun <T> inTransactionOf(
    connection: Connection,
    ignoreCommit: Boolean = false,
    statement: Transaction.() -> T
): T {
    val before = TransactionManager.manager
    TransactionManager.resetCurrent(ExistingConTxManager(
        con = connection,
        ignoreCommit = ignoreCommit,
    ))
    try {
        val res = TransactionManager.current().statement()
        closeStatements(TransactionManager.current())
        return res
    } finally {
        TransactionManager.resetCurrent(before)
    }
}

// copy-paste from internal closeStatementsAndConnection
private fun closeStatements(transaction: Transaction) {
    val currentStatement = transaction.currentStatement
    try {
        currentStatement?.let {
            it.closeIfPossible()
            transaction.currentStatement = null
        }
        transaction.closeExecutedStatements()
    } catch (e: Exception) {
        exposedLogger.warn("Statements close failed", e)
    }
}


internal class ExistingConTxManager(
    con: Connection,
    ignoreCommit: Boolean,
) : TransactionManager {
    
    private val trans =
        Transaction(object : TransactionInterface {
            
            override val connection = JdbcConnectionImpl(con)
            
            override val db: Database =
                Database.connect(
                    getNewConnection = { throw unsup() },
                    manager = { this@ExistingConTxManager },
                )
            
            override val outerTransaction: Transaction?
                get() = null
            override val transactionIsolation: Int
                get() = con.transactionIsolation
            
            override fun close() {
                throw unsup()
            }
            
            override fun commit() {
                if (!ignoreCommit) {
                    throw unsup()
                }
            }
            
            override fun rollback() {
                throw unsup()
            }
            
        })
    
    override var defaultIsolationLevel: Int
        get() = throw unsup()
        set(value) { throw unsup() }
    override var defaultRepetitionAttempts: Int
        get() = throw unsup()
        set(value) { throw unsup() }
    
    override fun bindTransactionToThread(transaction: Transaction?) {
        throw unsup()
    }
    
    override fun currentOrNull() =
        trans
    
    override fun newTransaction(isolation: Int, outerTransaction: Transaction?) =
        throw unsup()
    
    private fun unsup() = UnsupportedOperationException("inTransactionOf does not support this operation")
    
}