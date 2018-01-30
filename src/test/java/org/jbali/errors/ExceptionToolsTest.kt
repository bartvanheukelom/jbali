package org.jbali.errors

import org.junit.Test
import kotlin.test.assertEquals

class ExceptionToolsTest {

    @Test fun testRemoveCurrentStack() {

        val locTrace = Thread.currentThread().stackTrace
        // 0 = Thread.getStackTrace
        val trcs = locTrace[1]
        val caller = locTrace[2]

        try {
            alex()
        } catch (e: Exception) {
            e.removeCurrentStack()
            val sb = e.stackTrace.last()
            assertEquals(ExceptionToolsTest::class.qualifiedName, sb.className)
            assertEquals(::testRemoveCurrentStack.name, sb.methodName)
        }

        // catch adds to stack
        try {
            alex()
        } catch (e: Exception) {
            {
                e.removeStackFrom(trcs, caller)
                val sb = e.stackTrace.last()
                assertEquals(ExceptionToolsTest::class.qualifiedName, sb.className)
                assertEquals(::testRemoveCurrentStack.name, sb.methodName)
            }()
        }

        // should not corrupt the stack if it was already truncated
        try {
            cleanThrower()
        } catch (e: Exception) {
            e.removeCurrentStack()
            val sb = e.stackTrace.last()
            assertEquals(ExceptionToolsTest::class.qualifiedName, sb.className)
            assertEquals(::cleanThrower.name, sb.methodName)
        }

        // also check causes
        try {
            {
                try {
                    alex()
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }()
        } catch (e: Exception) {
            e.removeCurrentStack()
            for (c in e.causeChain) {
                val sb = c.stackTrace.last()
                assertEquals(ExceptionToolsTest::class.qualifiedName, sb.className)
                assertEquals(::testRemoveCurrentStack.name, sb.methodName)
            }
        }

    }

    private fun cleanThrower() {
        try {
            alex()
        } catch (e: Exception) {
            throw e.apply { removeCurrentStack() }
        }
    }

    private fun alex() = bert()
    private fun bert() = charles()
    private fun charles() = dick()
    private fun dick(): Unit = throw RuntimeException()

}