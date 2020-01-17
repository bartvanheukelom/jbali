package org.jbali.util

import com.google.common.base.Function
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import java.util.*
import kotlin.properties.ReadOnlyProperty
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
        by CachedExtensionProperty.ignoringReceiver(UUID::randomUUID)



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

class CachedExtensionProperty<in R : Any, out T>(getter: R.() -> T) : ReadOnlyProperty<R, T> {

    companion object {
        fun <R : Any, T> ignoringReceiver(generator: () -> T) =
                CachedExtensionProperty<R, T>(generator.ignoringReceiver())
    }

    val cache =
            weakKeyLoadingCache<R, Optional<out T>> {
                Optional.of(getter(it))
            }

    override fun getValue(thisRef: R, property: KProperty<*>): T =
            cache.invoke(thisRef).orElse(null)

}