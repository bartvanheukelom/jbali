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

        // removeCurrentStack
        try {
            alex()
        } catch (e: Exception) {
            e.removeCurrentStack()
            val sb = e.stackTrace.last()
            assertEquals(ExceptionToolsTest::class.qualifiedName, sb.className)
            assertEquals(::testRemoveCurrentStack.name, sb.methodName)
        }

        // withoutCurrentStack
        try {
            alex()
        } catch (e: Exception) {

            val last = e.stackTrace.last()

            e.withoutCurrentStack {
                val sb = e.stackTrace.last()
                assertEquals(ExceptionToolsTest::class.qualifiedName, sb.className)
                assertEquals(::testRemoveCurrentStack.name, sb.methodName)
            }

            assertEquals(last, e.stackTrace.last())
        }

        // catch adds to stack, so remove from pre-recorded signature
        try {
            alex()
        } catch (e: Exception) {
            runNonInline {
                e.removeStackFrom(sig)
                val sb = e.stackTrace.last()
                assertEquals(ExceptionToolsTest::class.qualifiedName, sb.className)
                assertEquals(::testRemoveCurrentStack.name, sb.methodName)
            }
        }

        // catch adds to stack, so remove common tail
        try {
            alex()
        } catch (e: Exception) {
            runNonInline {
                e.removeCommonStack()
                val sb = e.stackTrace.last()
                assertEquals(ExceptionToolsTest::class.qualifiedName, sb.className)
                assertEquals(::testRemoveCurrentStack.name, sb.methodName)
            }
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
            runNonInline {
                try {
                    alex()
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
        } catch (e: Exception) {
            
            System.err.println("Original:")
            // java.lang.RuntimeException: java.lang.RuntimeException: I'm being a...
            //	at org.jbali.errors.ExceptionToolsTest$testRemoveCurrentStack$6.invoke(ExceptionToolsTest.kt:98)
            //	at org.jbali.errors.ExceptionToolsTest$testRemoveCurrentStack$6.invoke(ExceptionToolsTest.kt:94)
            //	at org.jbali.errors.ExceptionToolsTestKt.runNonInline(ExceptionToolsTest.kt:136)
            //	at org.jbali.errors.ExceptionToolsTest.testRemoveCurrentStack(ExceptionToolsTest.kt:94)
            //	...
            //	at worker.org.gradle.process.internal.worker.GradleWorkerMain.main(GradleWorkerMain.java:74)
            //Caused by: java.lang.RuntimeException: I'm being a...
            //	at org.jbali.errors.ExceptionToolsTest.dick(ExceptionToolsTest.kt:132)
            //	...
            //	at org.jbali.errors.ExceptionToolsTest$testRemoveCurrentStack$6.invoke(ExceptionToolsTest.kt:96)
            //	... 46 more
            e.printStackTrace()
            
            e.removeCurrentStack()
            
            System.err.println("After removeCurrentStack:")
            // java.lang.RuntimeException: java.lang.RuntimeException: I'm being a...
            //	at org.jbali.errors.ExceptionToolsTest$testRemoveCurrentStack$6.invoke(ExceptionToolsTest.kt:98)
            //	at org.jbali.errors.ExceptionToolsTest$testRemoveCurrentStack$6.invoke(ExceptionToolsTest.kt:94)
            //	at org.jbali.errors.ExceptionToolsTestKt.runNonInline(ExceptionToolsTest.kt:136)
            //	at org.jbali.errors.ExceptionToolsTest.testRemoveCurrentStack(ExceptionToolsTest.kt:94)
            //Caused by: java.lang.RuntimeException: I'm being a...
            //	at org.jbali.errors.ExceptionToolsTest.dick(ExceptionToolsTest.kt:132)
            //	...
            //	at org.jbali.errors.ExceptionToolsTest$testRemoveCurrentStack$6.invoke(ExceptionToolsTest.kt:96)
            //	... 3 more
            e.printStackTrace()
            
            for ((i, c) in e.causeChain.withIndex()) {
                val sb = c.stackTrace.last()
                System.err.println("Cause $i, sb=$sb")
                // all stack entries higher than the current one should have been removed,
                // i.e. the last one should be the current method.
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
    private fun dick(): Unit = throw RuntimeException("I'm being a...")

}

fun runNonInline(body: () -> Unit) = body()
