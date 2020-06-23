package org.jbali.util

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Base interface that can be used for implementing property delegate *providers* of read-only properties.
 *
 * [https://kotlinlang.org/docs/reference/delegated-properties.html#providing-a-delegate-since-11]
 *
 * This is provided only for convenience; you don't have to extend this interface
 * as long as your property delegate has methods with the same signatures.
 *
 * @param R the type of object which owns the delegated property.
 * @param T the type of the property value.
 */
interface ReadOnlyPropertyProvider<in R, out T> {
    operator fun provideDelegate(thisRef: R, property: KProperty<*>): ReadOnlyProperty<R, T>
}

/**
 * Read-only property delegate that returns the given fixed value.
 */
data class FixedValueDelegate<T>(val value: T) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}

/**
 * Property delegate provider that calls [initer] with a reference to the property and
 * provides the property with a delegate that will always return [initer]'s return value.
 *
 * ```
 * val del = InitFixedValueDelegate<String> { prop ->
 *   System.getProperties().getValue("config.${prop.name}").toString()
 * }
 * val config = object {
 *   val env  by del
 *   val user by del
 * }
 * ```
 */
data class InitFixedValueDelegate<T>(val initer: (KProperty<*>) -> T): ReadOnlyPropertyProvider<Any?, T> {
    override operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): FixedValueDelegate<T> =
            FixedValueDelegate(try {
                initer(property)
            } catch (e: Throwable) {
                throw RuntimeException("Could not init property $property of $thisRef: $e", e)
            })
}

data class PrintingFixedValueDelegate<T>(val value: T): ReadOnlyPropertyProvider<Any?, T> {
    override fun provideDelegate(thisRef: Any?, property: KProperty<*>): ReadOnlyProperty<Any?, T> {
        println("${property.name} = $value")
        return FixedValueDelegate(value)
    }
}


/**
 * Decorate this function to also accept, and ignore, a receiver of type [R].
 */
// TODO ask if there isn't any simpler way
@Suppress("NOTHING_TO_INLINE")
inline fun <R, T> (() -> T).ignoringReceiver(): (R.() -> T) {
    val t: () -> T = this
    return { t() }
}
