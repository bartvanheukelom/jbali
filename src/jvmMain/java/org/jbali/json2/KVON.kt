package org.jbali.json2

import org.jbali.collect.forEachEntryIndexed

/**
 * Key-Value Object Notation
 *
 * A simple translation of JSON that makes JSON objects a bit more convenient to express as a list of key and value
 * strings, e.g. as URL query string arguments (`application/x-www-form-urlencoded`) or Java properties.
 *
 * The conversion rules from KVON to a JSON object are:
 * - Input iteration order is preserved in the resulting object.
 * - Keys are quoted.
 * - Values that start with '[' and '{' are assumed to be in JSON format and are included verbatim.
 *   Any illegal JSON here will cause the result to be illegal as well.
 * - Other values are quoted. This also implies that this conversion will never output `null`, numbers or booleans.
 * - TODO? `list[]=1, list[]=2` -> `"list": ["1", "2"]`
 * - TODO? `obj.x=1, obj.y=2` -> `"obj": {"x": "1", "y": "2"]`
 *
 * TODO do similar standardized formats exist? -> perhaps the Properties format of kotlinx.serialization
 *
 * TODO how hard would it be to write a custom kotlin-serialization format for this?
 *
 */
sealed class KVON {

    val asJsonObject: JSONString by lazy {
        toJson()
    }

    protected abstract fun toJson(): JSONString

    override fun toString() = asJsonObject.toString()
    override fun equals(other: Any?) = other is KVON && other.asJsonObject == asJsonObject
    override fun hashCode() = asJsonObject.hashCode()

    class Map(
            val map: kotlin.collections.Map<String, String>
    ) : KVON() {
        override fun toJson(): JSONString =
                map.entries.toJson(
                        key   = { it.key },
                        value = { it.value }
                )
    }

    class Pairs(
            val entries: Iterable<Pair<String, String>>
    ) : KVON() {

        constructor(vararg entries: Pair<String, String>) :
                this(entries = entries.asIterable())

        override fun toJson(): JSONString =
                entries.toJson(
                        key   = { it.first },
                        value = { it.second }
                )
    }

    protected inline fun <E : Any> Iterable<E>.toJson(
            key: (E) -> String,
            value: (E) -> String
    ): JSONString =
            JSONString(buildString(
                    capacity = 2048 + 1024 // maximum URL length is 2048, JSON takes more space
            ) {
                append('{')
                forEachEntryIndexed(key, value) { i, k, v ->
                    if (i != 0) {
                        append(',')
                    }
                    appendJsonQuoted(k)
                    append(':')
                    when (v.first()) {
                        '[', '{' -> append(v)
                        else -> appendJsonQuoted(v)
                    }
                }
                append('}')
            })

}
