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
    try {
        ExistingConTxManager(
            con = connection,
            ignoreCommit = ignoreCommit,
        ).use { ectm ->
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
                closeStatements(tx)
            }
        }
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


//internal - TODO enable when liveInstances not checked by external tests
class ExistingConTxManager(
    con: Connection,
    ignoreCommit: Boolean,
) : TransactionManager, AutoCloseable {
    
    companion object {
        var liveInstances = 0
    }
    
    // TODO use safeInit
    private val db: Database =
        Database.connect(
            getNewConnection = { throw unsup() },
            manager = { this@ExistingConTxManager },
        )
    
    private val trans =
        Transaction(object : TransactionInterface {
            
            override val connection = JdbcConnectionImpl(con)
            
            override val db get() = this@ExistingConTxManager.db
            
            override val outerTransaction: Transaction? get() = null
            override val transactionIsolation: Int get() = con.transactionIsolation
            
            override fun close() {}
            
            override fun commit() {
//                if (!ignoreCommit) {
//                    throw unsup()
//                }
            }

            override fun rollback() {
                con.rollback()
            }
            
        })
    
    init { liveInstances++ }
    
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
    
    override fun close() {
        // undo call to TransactionManager.registerManager that was performed by Database.connect
        TransactionManager.closeAndUnregister(db)
        liveInstances--
    }
    
}
