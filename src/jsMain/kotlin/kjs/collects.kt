@file:JsExport
package org.jbali.kjs

import kotlin.js.JsExport

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

    fun <T> ktForEach(ite: Iterable<T>, action: (T) -> Unit) {
        ite.forEach(action)
    }

    fun <T> iterableToArray(ite: Iterable<T>): Array<T> {
        @Suppress("UNUSED_VARIABLE")
        val al = if (ite is ArrayList) ite else ArrayList<T>().also { it.addAll(ite) }
        return js("al.toArray()")
    }

}

fun <T> emptyKtList() = emptyList<T>()
