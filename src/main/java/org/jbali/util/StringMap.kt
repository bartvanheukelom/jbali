package org.jbali.util

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties


/**
 * A map wrapper with string keys, which it knows at runtime too.
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
 */
fun objToMap(obj: Any) =
        StringMap(
                obj.javaClass.kotlin.declaredMemberProperties.map {
                    it.name to it.get(obj)
                }.toMap()
        )


data class ClassMapInfo<T : Any>(val clazz: KClass<T>) {
    val props = clazz.memberProperties
            .filter { it.visibility == KVisibility.PUBLIC }
            .associateBy { it.name }
    val keys = props.keys
}

val <T : Any> KClass<T>.classMapInfo: ClassMapInfo<T> by StoredExtensionProperty(::ClassMapInfo)

/**
 * Present the public member properties of any object as a Map. Uses the given object for storage, so take
 * care if it is mutable.
 * TODO move to jbali
 */
class ObjMap<T : Any>(
        private val ci: ClassMapInfo<T>,
        val obj: T
) : Map<String, Any?> {
    @Suppress("UNCHECKED_CAST")
    constructor(obj: T): this(
            obj.javaClass.kotlin.classMapInfo,
            obj)

    override val keys = ci.keys
    override val size = ci.keys.size

    override val entries: Set<Map.Entry<String, Any?>> =
            ci.props.map {
                object : Map.Entry<String, Any?> {
                    override val key = it.key
                    override val value = it.value.get(obj)
                }
            }.toSet()

    override val values =
            ci.props.map {
                it.value.get(obj)
            }

    override fun containsKey(key: String) = ci.props.containsKey(key)

    override fun containsValue(value: Any?) =
            ci.props.values.any {
                it.get(obj) == value
            }

    override fun get(key: String) = ci.props[key]?.get(obj)
    override fun isEmpty() = ci.props.isEmpty()

    // simplest correct implementation. inefficient, but who uses this anyway?
    override fun equals(other: Any?) = other is Map<*, *> && toMap() == other
    override fun hashCode() = toMap().hashCode()

}
