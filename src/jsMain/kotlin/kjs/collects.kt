@file:JsExport
package org.jbali.kjs

// utility wrappers to deal with Kotlin collections in JavaScript code

data class KtMap<K, out V>(
        val map: Map<K, V>
) {

    fun forEach(action: (K, V) -> Unit) {
        map.forEach {
            action(it.key, it.value)
        }
    }

}

data class KtIterable<out T>(
        val ite: Iterable<T>
) {

    fun forEach(action: (T) -> Unit) {
        ite.forEach(action)
    }

    /**
     * Get the contents of the iterable as a native JavaScript array.
     * Possibly returns a reference to the existing array that holds the iterable's data,
     * so be careful about mutating the return value.
     */
    fun toArray(): Array<@UnsafeVariance T> =
            // NOTE: this implementation in fact always returns a copy,
            //       but the doc should still reserve the right to change that
            when (ite) {
                is Collection -> ite.toTypedArray()
                else -> ite.toList().toTypedArray()
            }

}

fun <T> emptyKtList() = emptyList<T>()
