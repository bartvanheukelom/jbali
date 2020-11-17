@file:JsExport
@file:Suppress("NON_EXPORTABLE_TYPE", "unused")

package org.jbali.kjs

// utility wrappers to deal with Kotlin collections in JavaScript code

data class KtMap<K, out V>(
        /**
         * The Kotlin [Map] that this wraps around.
         */
        val kMap: Map<K, V>
) {

    @Deprecated("", ReplaceWith("kMap"))
    val map get() = kMap

    fun forEach(action: (K, V) -> Unit) {
        kMap.forEach {
            action(it.key, it.value)
        }
    }

    /**
     * Get the contents of the map as a plain native JavaScript object.
     * Non-string keys are converted using `toString`.
     *
     * Possibly returns a reference to the existing object that holds the map data,
     * so be careful about mutating the return value.
     */
    fun toObject(): dynamic =
            // NOTE: this implementation in fact always returns a copy,
            //       but the doc should still reserve the right to change that
            js("({})").also { o ->
                kMap.forEach { (k, v) ->
                    o[k.toString()] = v
                }
            }

}

open class KtIterable<out T>(
        /**
         * The Kotlin [Iterable] that this wraps around.
         */
        val kIterable: Iterable<T>
) {

    @Deprecated("", ReplaceWith("kIterable"))
    @JsName("ite")
    val deprecatedIte get() = kIterable

    /**
     * Performs the given [action] on each element.
     */
    fun forEach(action: (T) -> Unit) {
        kIterable.forEach(action)
    }

    /**
     * Get the contents of the iterable as a native JavaScript array.
     * Possibly returns a reference to the existing array that holds the iterable's data,
     * so be careful about mutating the return value.
     */
    fun toArray(): Array<@UnsafeVariance T> =
            // NOTE: this implementation in fact always returns a copy,
            //       but the doc should still reserve the right to change that
            when (kIterable) {
                is Collection -> kIterable.toTypedArray()
                else -> kIterable.toList().toTypedArray()
            }

    /**
     * Returns a [List] containing all elements. May or may not be a copy of [kIterable].
     */
    open fun toKList() = kIterable.toList()

    /**
     * Returns a [Set] containing all elements. May or may not be a copy of [kIterable].
     * Duplicate elements are silently discarded.
     */
    open fun toKSet() = kIterable.toSet()

}

class KtList<out T>(
        kList: List<T>
) : KtIterable<T>(kList) {

    /**
     * [kIterable], cast to [List].
     */
    val kList get() = kIterable as List<T>

    /**
     * @return [kList]
     */
    override fun toKList(): List<T> = kList

    @JsName("empty")
    constructor() : this(emptyList())

    /**
     * Create a list from a native JavaScript array.
     *
     * From JavaScript: `KtList.fromArray([...])`
     */
    @JsName("fromArray")
    constructor(array: Array<T>) : this(array.toList())

}

class KtSet<out T>(
        kSet: Set<T>
) : KtIterable<T>(kSet) {

    /**
     * [kIterable], cast to [Set].
     */
    val kSet get() = kIterable as Set<T>

    /**
     * @return [kSet]
     */
    override fun toKSet() = kSet

    /**
     * Returns a wrapped empty set.
     *
     * From JavaScript: `KtSet.empty()`
     */
    @JsName("empty")
    constructor() : this(emptySet())

    /**
     * Create a set from a native JavaScript array.
     * Duplicate elements are silently discarded.
     *
     * From JavaScript: `KtSet.fromArray([...])`
     */
    @JsName("fromArray")
    constructor(array: Array<T>) : this(array.toSet())

}

/**
 * JavaScript replacement: `KtList.empty().kList`
 */
@Deprecated("", ReplaceWith("KtList<T>()", "org.jbali.kjs.KtList"))
fun <T> emptyKtList() = KtList<T>()
