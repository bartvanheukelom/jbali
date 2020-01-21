package org.jbali.util

import com.google.common.base.Function
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface UUIdentifiable {
    val uuid: UUID
}

/** @return The UUID of the receiver if it natively has one, otherwise one that is linked to the object's JVM identity. */
val Any.uuid: UUID get() =
    if (this is UUIdentifiable) uuid
    else objectIdentityUUID

/** @return A UUID that is linked to, and unique to, the receiver's JVM identity. */
val Any.objectIdentityUUID: UUID
        by StoredExtensionProperty.ignoringReceiver(UUID::randomUUID)



// utils

fun <K : Any, V : Any> weakKeyLoadingCache(loader: (K) -> V): (K) -> V =
        CacheBuilder.newBuilder()
                .weakKeys()
                .build(CacheLoader.from(Function<K, V> {
                    loader(it!!)
                }))::get

// TODO ask if there isn't any simpler way
@Suppress("NOTHING_TO_INLINE")
inline fun <R, T> (() -> T).ignoringReceiver(): (R.() -> T) {
    val t: () -> T = this
    return { t() }
}

private val extensionPropertyStorage =
        weakKeyLoadingCache<Any, ConcurrentHashMap<Any, Any>> {
            ConcurrentHashMap()
        }

/**
 * Delegate to add a mutable extension property to an object, whose value is stored in a weak-keyed identity-based map.
 */
@Suppress("UNCHECKED_CAST")
class StoredExtensionProperty<in R : Any, T : Any>(
        private val initialValue: R.() -> T
) : ReadWriteProperty<R, T> {

    companion object {
        fun <R : Any, T : Any> ignoringReceiver(initialValue: () -> T) =
                StoredExtensionProperty<R, T>(initialValue.ignoringReceiver())
    }

    // TODO provideDelegate, check if property is indeed an extension, and ensure exactly 1 delegate per prop

    override fun getValue(thisRef: R, property: KProperty<*>): T =
        extensionPropertyStorage(thisRef).getOrPut(this) {
            initialValue(thisRef)
        } as T

    override fun setValue(thisRef: R, property: KProperty<*>, value: T) {
        extensionPropertyStorage(thisRef)[this] = value
    }

}
