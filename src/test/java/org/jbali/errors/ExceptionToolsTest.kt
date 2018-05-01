package org.jbali.errors

import org.junit.Test
import kotlin.test.assertEquals

class ExceptionToolsTest {

    @Test fun testCommonTail() {
        assertEquals(listOf("a", "b", "c"), commonTail(listOf("a", "b", "c"), listOf("a", "b", "c")))

        assertEquals(listOf("b", "c"), commonTail(listOf("x", "b", "c"), listOf("y", "b", "c")))
        assertEquals(listOf("b", "c"), commonTail(listOf("b", "c"), listOf("y", "b", "c")))
        assertEquals(listOf("b", "c"), commonTail(listOf("b", "c"), listOf("b", "c")))

        assertEquals(listOf("c"), commonTail(listOf("b", "c"), listOf("g", "c")))

        assertEquals(listOf("a", "b", "c"), commonHead(listOf("a", "b", "c"), listOf("a", "b", "c")))

        assertEquals(listOf("a", "b"), commonHead(listOf("a", "b", "c"), listOf("a", "b", "x")))
        assertEquals(listOf("a", "b"), commonHead(listOf("a", "b"), listOf("a", "b", "c")))
        assertEquals(listOf("a", "b"), commonHead(listOf("a", "b", "c"), listOf("a", "b")))

        assertEquals(listOf("c"), commonHead(listOf("c", "c"), listOf("c", "g")))
    }

    @Test fun testRemoveCurrentStack() {

        val sig = currentStackSignature()!!

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
                e.removeStackFrom(sig)
                val sb = e.stackTrace.last()
                assertEquals(ExceptionToolsTest::class.qualifiedName, sb.className)
                assertEquals(::testRemoveCurrentStack.name, sb.methodName)
            }()
        }

        // catch adds to stack (use removecommon)
        try {
            alex()
        } catch (e: Exception) {
            {
                e.removeCommonStack()
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