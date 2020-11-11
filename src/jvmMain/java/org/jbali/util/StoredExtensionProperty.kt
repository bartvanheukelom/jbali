package org.jbali.util

import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Delegate to add a mutable extension property to an object, whose value is stored in a weak-keyed identity-based map.
 */
@Suppress("UNCHECKED_CAST")
// TODO remove Any bound on T (allow null values), but how to do that without excessive wrapping? maybe guava mapbuilder supports null values
class StoredExtensionProperty<in R, T : Any>(
        private val initialValue: R.() -> T
) : ReadWriteProperty<R, T> {

    companion object {
        /**
         * Create a [StoredExtensionProperty] whose value does not depend on the receiver object,
         * but can still be distinct for different objects (so not static / on the companion).
         * For example, a randomly generated ID.
         */
        fun <R, T : Any> ignoringReceiver(initialValue: () -> T) =
                StoredExtensionProperty<R, T>(initialValue.ignoringReceiver())
    }

    // TODO provideDelegate, check if property is indeed an extension, and ensure exactly 1 delegate per prop

    private val nullKey = Any()

    override fun getValue(thisRef: R, property: KProperty<*>): T =
        extensionPropertyStorage(thisRef ?: nullKey).getOrPut(this) {
            initialValue(thisRef)
        } as T

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        extensionPropertyStorage(thisRef ?: nullKey)[this] = value
    }

}


private val extensionPropertyStorage =
        weakKeyLoadingCache<Any, ConcurrentHashMap<Any, Any>> {
            // TODO
            ConcurrentHashMap()
        }
