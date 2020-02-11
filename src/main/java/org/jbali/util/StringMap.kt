package org.jbali.util

import kotlinx.serialization.Serializable
import kotlin.reflect.full.declaredMemberProperties


/**
 * A map wrapper with string keys, which it knows at runtime too.
 * TODO make interface and let ObjMap implement it too
 */
@Serializable
class StringMap<V>(val contents: Map<String, V>) : Map<String, V> by contents {
    constructor(vararg entries: Pair<String, V>): this(mapOf(*entries))
    override fun toString() = "StringMap$contents"
}

/**
 * Turn an anonymous object into a StringMap.
 * Example:
 * objToMap(object {
 *     val foo = "hi"
 *     val bar = 12
 *     val nest = objToMap(object {
 *         val birdy = "tweet"
 *     })
 * })
 * Will shallowly copy the data in the object to a new map.
 *
 * TODO deprecate because ObjMap is better
 */
fun objToMap(obj: Any) =
        StringMap(
                obj.javaClass.kotlin.declaredMemberProperties.map {
                    it.name to it.get(obj)
                }.toMap()
        )
