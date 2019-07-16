package org.jbali.collect

/**
 * Returns a new map that is nearly identical to this map, except the new value associated with [key]
 * will be the result of applying [transform] to that value in this map.
 *
 * If [key] is not present in this map, the returned map will be a copy.
 *
 * The returned map preserves the entry iteration order of the original map.
 */
fun <K, V> Map<K, V>.mapSingleEntry(key: K, transform: (V) -> V) =
        mapValues {
            if (it.key == key) transform(it.value)
            else it.value
        }
