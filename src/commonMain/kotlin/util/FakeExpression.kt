package org.jbali.util

import kotlin.jvm.JvmStatic

// TODO 2 concepts are intermingled here. split them up cleanly:
//      - fake testing exceptions that test error handling
//      - fake expressions that coerce the compiler to compile something that is guaranteed to fail

@Suppress("FunctionName")
object FakeExpression {

    fun <T> TODO(name: String = "<unnamed>"): T =
        throw NotImplementedException("This expression $name is not yet implemented")

    fun <T> exceptionForTest(name: String = "Expression"): T =
        throw FakeException("$name throws a fake exception for testing")

    fun <T> errorForTest(name: String = "Expression"): T =
        throw FakeError("$name throws a fake error for testing")
    
    fun <T> of(): T =
        throw RuntimeException("This code must not be called")

    @JvmStatic fun TODO_TRUE(): Boolean = true
    @JvmStatic fun TODO_FALSE(): Boolean = false

}

/**
 * Satisfies the compiler with a value of type T, but at runtime throws an exception.
 * For use in no-arg constructors that must exist but not actually used, e.g. to satisfy
 * JAXB for classes that are only supossed to be marshalled to XML, not the other way around.
 */
fun <T> fakeConstructorValue(): T =
    throw RuntimeException("Using this constructor is not actually supported")

/**
 * Marker interface that is implemented by [FakeException] and [FakeError].
 */
interface FakeThrowable

/**
 * A [RuntimeException] that isn't thrown due to a real error condition, but on purpose
 * (randomly or otherwise), to test error handling.
 */
open class FakeException(msg: String): RuntimeException(msg), FakeThrowable

/**
 * An [Error] that isn't thrown due to a real error condition, but on purpose
 * (randomly or otherwise), to test error handling.
 */
open class FakeError(msg: String): Error(msg), FakeThrowable

/**
 * A [RuntimeException] that is thrown to indicate that a method body remains to be implemented.
 * Complements the built-in [NotImplementedError] which is an [Error].
 */
open class NotImplementedException(msg: String): RuntimeException(msg)
