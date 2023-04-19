package org.jbali.spring

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.UnsatisfiedDependencyException


/**
 * Returns an instance of the object managed by this factory,
 * or `null` if not available. Unlike [ObjectProvider.getIfAvailable],
 * "not available" includes the case where a bean of the required type
 * has been defined but cannot be created due to unsatisfied dependencies,
 * i.e. if [UnsatisfiedDependencyException] is thrown.
 * Other exceptions are rethrown.
 */
fun <T> ObjectProvider<T>.orNull(): T? =
    try {
        ifAvailable
    } catch (ude: UnsatisfiedDependencyException) {
        null
    }


/**
 * Create an [ObjectProvider] that always returns the receiving object.
 * Can be used when manually constructing an object that is normally
 * constructed by Spring, e.g. in a test.
 *
 * The returned [ObjectProvider] does not support multi-element access.
 */
fun <T> T.provider(): ObjectProvider<T> =
    object : ObjectProvider<T> {
        private val obj = this@provider
        override fun getObject(vararg args: Any?): T {
            require(args.isEmpty())
            return obj
        }
        override fun getObject(): T = obj
        override fun getIfAvailable(): T? = obj
        override fun getIfUnique(): T? = obj
    }

/**
 * Create an [ObjectProvider] that returns a single object, which is
 * supplied by the given [supplier] function.
 * The supplier is called at most once, when the object is first requested.
 * After that, the same object is returned.
 *
 * The returned [ObjectProvider] does not support multi-element access.
 */
fun <T> objectProvider(supplier: () -> T): ObjectProvider<T> =
    object : ObjectProvider<T> {
        private val obj by lazy(supplier)
        override fun getObject(vararg args: Any?): T {
            require(args.isEmpty())
            return obj
        }
        override fun getObject(): T = obj
        override fun getIfAvailable(): T? = obj
        override fun getIfUnique(): T? = obj
    }


/**
 * An [ObjectProvider] that always returns `null`.
 */
@Suppress("WRONG_NULLABILITY_FOR_JAVA_OVERRIDE")
object NullProvider : ObjectProvider<Any?> {
    override fun getObject(vararg args: Any?): Any? {
        require(args.isEmpty())
        return null
    }
    override fun getObject(): Any? = null
    override fun getIfAvailable(): Any? = null
    override fun getIfUnique(): Any? = null
}
