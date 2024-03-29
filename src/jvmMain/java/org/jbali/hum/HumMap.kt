package org.jbali.hum

import org.jbali.collect.ListSet

/**
 * Optimized [Map] implementation that contains a value for each entry of the [HumValue] denoted by [type].
 */
class HumMap<K : HumValue<K>, out V> private constructor(
    val type: HumNode<K, K>,
    override val values: List<V>
) : Map<K, V>, List<V> by values {
    
    companion object {
        /**
         * Create a HumMap for the given [type] with the given associated [values].
         * The caller is responsible for ensuring these are in the same order as the [HumNode.values].
         */
        fun <K : HumValue<K>, V> fromOrderedValues(type: HumNode<K, K>, values: List<V>) = HumMap(type, values)
    
        fun <K : HumValue<K>, V> associate(type: HumNode<K, K>, valueGetter: (K) -> V) =
            fromOrderedValues(type, type.map(valueGetter))
    }

    init {
        require(values.size == type.values.size)
    }

    override fun toString() =
            toMap().toString() // TODO optimize

    override val entries: Set<Map.Entry<K, V>>
        get() = type.values.associateWith(this::get).entries // TODO optimize

    override val keys: ListSet<K>
        get() = type.values

    override val size: Int
        get() = type.values.size

    override fun containsKey(key: K) = true

    override fun containsValue(value: @UnsafeVariance V) =
            value in values

    override fun get(key: K): V =
            values[key.ordinal]

    override fun isEmpty() = false

    override fun equals(other: Any?) =
            when (other) {
                is HumMap<*, *> ->
                    type == other.type && values == other.values
                is Map<*, *> ->
                    size == other.size && toMap() == other
                else ->
                    false
            }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + values.hashCode()
        return result
    }

}
