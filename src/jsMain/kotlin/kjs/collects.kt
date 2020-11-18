@file:JsExport
@file:Suppress("NON_EXPORTABLE_TYPE", "unused")

package org.jbali.kjs

import org.jbali.util.WorksAroundBaseNameMangling

// utility wrappers to deal with Kotlin collections in JavaScript code
// TODO add docs to classes. mention they are just wrappers, not List etc themselves


/**
 * Wrap this around a Kotlin [Map] to make it easier to process from JavaScript code, or to convert it to a JavaScript object or array.
 *
 * Static methods of this class can be used to create a new map, e.g. an empty one, or from an array.
 *
 * Note that instances of this class aren't a [Map] themselves, and can't be passed to functions requiring one.
 * Instead, pass them [kMap].
 */
data class KtMap<K, out V>(
        /**
         * The Kotlin [Map] that this wraps around.
         */
        val kMap: Map<K, V>
) {

    @Deprecated("", ReplaceWith("kMap"))
    @JsName("map")
    val deprecatedMap: Nothing get() = throw NotImplementedError("this property will be removed, use kMap")

    override fun toString(): String =
            "KtMap$kMap"

    @JsName("forEach")
    // TODO feature request: let key and value names be output to d.ts, or no names (but not p0 etc)
    fun forEach(action: (key: K, value: V) -> Unit) {
        kMap.forEach {
            action(it.key, it.value)
        }
    }

    /**
     * Get the contents of the iterable as a native JavaScript array of [key, value] tuples.
     * Possibly returns a reference to the existing array that holds the map data,
     * so be careful about mutating the return value.
     *
     * TODO patch the generated d.ts to add proper typing to the tuples
     */
    @JsName("toArray") // is this required because of the non-exportable return type?
    fun toArray(): Array<Array<Any?>> =
            // NOTE: this implementation in fact always returns a copy,
            //       but the doc should still reserve the right to change that
            kMap.entries.asSequence()
                    .map { (k, v) ->
                        arrayOf(k, v)
                    }
                    .toList()
                    .toTypedArray()

    /**
     * Get the contents of the map as a plain native JavaScript object.
     * Non-string keys are converted using `toString`.
     *
     * Possibly returns a reference to the existing object that holds the map data,
     * so be careful about mutating the return value.
     */
    @JsName("toObject")
    fun toObject(): dynamic {
        val o = js("({})")
        kMap.forEach { (k, v) ->
            o[k.toString()] = v
        }
        return o
    }


    @JsName("empty")
    constructor() : this(emptyMap())

    /**
     * Create a map from a native JavaScript array of [key, value] tuples.
     *
     * From JavaScript: `KtMap.fromArray([["foo", 1], ["bar", 2], ...])`
     *
     * TODO patch the generated d.ts to add proper typing to the tuples
     */
    @JsName("fromArray")
    constructor(array: Array<Array<*>>) : this(array.associate {
        @Suppress("UNCHECKED_CAST")
        Pair(it[0] as K, it[1] as V)
    })

}

open class KtIterable<out T>(
        /**
         * The Kotlin [Iterable] that this wraps around.
         */
        val kIterable: Iterable<T>
) {

    protected open val variant: String get() = "KtIterable"

    @Deprecated("", ReplaceWith("kIterable"))
    @JsName("ite")
    val deprecatedIte: Nothing get() = throw NotImplementedError("this property will be removed, use kIterable")

    override fun toString(): String =
            "${variant}${kIterable}"

    override fun equals(other: Any?) =
            when (other) {
                is KtIterable<*> -> other.variant == variant && other.kIterable == kIterable
                else -> false
            }

    override fun hashCode(): Int =
            kIterable.hashCode()

    /**
     * Performs the given [action] on each element.
     */
    @JsName("forEach") @WorksAroundBaseNameMangling
    fun forEach(action: (element: T) -> Unit) {
        kIterable.forEach(action)
    }

    /**
     * Get the contents of the iterable as a native JavaScript array.
     * Possibly returns a reference to the existing array that holds the iterable's data,
     * so be careful about mutating the return value.
     */
    @JsName("toArray") @WorksAroundBaseNameMangling
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
    @JsName("toKList") @WorksAroundBaseNameMangling
    open fun toKList() = kIterable.toList()

    /**
     * Returns a [Set] containing all elements. May or may not be a copy of [kIterable].
     * Duplicate elements are silently discarded.
     */
    @JsName("toKSet") @WorksAroundBaseNameMangling
    open fun toKSet() = kIterable.toSet()

}

/**
 * Wrap this around a Kotlin [List] to make it easier to process from JavaScript code, or to convert it to a JavaScript array.
 *
 * Static methods of this class can be used to create a new list, e.g. an empty one, or from an array.
 *
 * Note that instances of this class aren't a [List] themselves, and can't be passed to functions requiring one.
 * Instead, pass them [kList].
 */
class KtList<out T>(
        kList: List<T>
) : KtIterable<T>(kList) {

    override val variant: String get() = "KtList"

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

/**
 * Wrap this around a Kotlin [Set] to make it easier to process from JavaScript code, or to convert it to a JavaScript array.
 *
 * Static methods of this class can be used to create a new set, e.g. an empty one, or from an array.
 *
 * Note that instances of this class aren't a [Set] themselves, and can't be passed to functions requiring one.
 * Instead, pass them [kSet].
 */
class KtSet<out T>(
        kSet: Set<T>
) : KtIterable<T>(kSet) {

    override val variant: String get() = "KtSet"

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
@Deprecated("", ReplaceWith("KtList<T>().kList", "org.jbali.kjs.KtList"))
fun <T> emptyKtList() = KtList<T>()
