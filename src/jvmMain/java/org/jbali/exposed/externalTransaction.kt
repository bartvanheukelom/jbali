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
    val ectm = ExistingConTxManager(
        con = connection,
        ignoreCommit = ignoreCommit,
    )
    TransactionManager.resetCurrent(ectm)
    val tx = TransactionManager.current()
    try {
        
        val res = tx.statement()
        
        // does not actually commit, but is required to run interceptors
        // that e.g. flush entity inserts
        tx.commit()
        
        return res
        
    } catch (e: Throwable) {
        val currentStatement = tx.currentStatement
        try {
            tx.rollback()
        } catch (e: Exception) {
            exposedLogger.warn("Transaction rollback failed: ${e.message}. Statement: $currentStatement", e)
        }
        throw e
    } finally {
        TransactionManager.resetCurrent(before)
        closeStatements(tx)
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
//                throw unsup()
            }
            
            override fun commit() {
//                if (!ignoreCommit) {
//                    throw unsup()
//                }
            }

            override fun rollback() {
                con.rollback()
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