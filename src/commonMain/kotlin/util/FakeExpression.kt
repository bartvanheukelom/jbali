package org.jbali.util

import kotlin.jvm.JvmStatic

@Suppress("FunctionName")
object FakeExpression {

    fun <T> TODO(name: String = "<unnamed>"): T =
            throw RuntimeException("This expression $name is not yet implemented") // TODO NotImplementedEXCEPTION (kotlin only has Error)

    fun <T> exceptionForTest(name: String = "Expression"): T =
            throw RuntimeException("$name throws a fake exception for testing")

    fun <T> errorForTest(name: String = "Expression"): T =
        throw Error("$name throws a fake error for testing")
    
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

