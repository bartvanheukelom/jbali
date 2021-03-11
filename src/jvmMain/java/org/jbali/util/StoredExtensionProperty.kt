package org.jbali.util

import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private typealias ExtPropReceiver = Any

/**
 * Delegate to add a mutable extension property to an object, whose value is stored in a weak-keyed identity-based map.
 * Once a value is stored, it is never removed as long as the object exists.
 *
 * NOTE: the map keeps a weak reference to the object and a strong reference to the value. If the value itself retains a
 * strong reference to the object, the object and value will never be garbage collected.
 * This is why [initer] receives the object [Borrowed].
 *
 * TODO make versions that do clear values, e.g. by using soft references or other rules supported by the guava cache
 */
@Suppress("UNCHECKED_CAST")
// TODO remove Any bound on T (allow null values), but how to do that without excessive wrapping? maybe guava mapbuilder supports null values
class StoredExtensionProperty<in R, T : Any>(
        private val initer: Borrowed<R>.() -> T
) : ReadWriteProperty<R, T> {

    companion object {
        /**
         * Create a [StoredExtensionProperty] whose value does not depend on the receiver object,
         * but can still be distinct for different objects (so not static / on the companion).
         * For example, a randomly generated ID.
         */
        fun <R, T : Any> ignoringReceiver(initer: () -> T) =
                StoredExtensionProperty<R, T>(initer.withIgnoredReceiver())
    }

    // TODO provideDelegate, check if property is indeed an extension, and ensure exactly 1 delegate per prop

    private val nullKey = Any()

    override fun getValue(thisRef: R, property: KProperty<*>): T =
        extensionPropertyStorage(thisRef ?: nullKey).getOrPut(this) {
            initer(loan(thisRef))
        } as T

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        extensionPropertyStorage(thisRef ?: nullKey)[this] = value
    }

}

// _all_ stored extension props are stored in this single cache (or at least, 1 per copy of jbali).
// this also means that StoredExtensionProperty instances are never garbage collected!
private val extensionPropertyStorage =
        weakKeyLoadingCache<ExtPropReceiver, ConcurrentHashMap<StoredExtensionProperty<*, *>, Any>> {
            // TODO .. what?
            ConcurrentHashMap()
        }
