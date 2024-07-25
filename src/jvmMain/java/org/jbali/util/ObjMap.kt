package org.jbali.util

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

/**
 * Present the public member properties of any object as a Map.
 * Doesn't copy anything, it's just a facade over the given object,
 * so it inherits mutability and thread-safety from that object.
 */
class ObjMap<T : Any> private constructor(
    private val clazz: ClassMapInfo<T>,
    val obj: T
) : Map<String, Any?> { // TODO MutableMap

    constructor(obj: T): this(
        obj.javaClass.kotlin.classMapInfo,
        obj
    )

    override val keys get() = clazz.keys
    override val size get() = clazz.keys.size

    // TODO only on get, return implementation that already has templates in ClassMapInfo
    override val entries: Set<Map.Entry<String, Any?>> get() =
        clazz.props.map {
            object : Map.Entry<String, Any?> {
                override val key get() = it.key
                override val value get() = it.value.get(obj)

                // TODO the following is the same for all implementations of Map.Entry, should be centralized somewhere (probably already in a lib)

                override fun equals(other: Any?) =
                    other is Map.Entry<*, *> &&
                        key == other.key &&
                        value == other.value

                override fun hashCode(): Int =
                    try {
                        key.hashCode() xor value.hashCode()
                    } catch (so: StackOverflowError) {
                        throw IllegalStateException("Stack overflow in ObjMap.hashCode(). This may be due to the mapped object having a getter that returns ObjMap(this). Make that a function instead.")
                    }

                override fun toString(): String =
                    try {
                        "$key=$value"
                    } catch (so: StackOverflowError) {
                        throw IllegalStateException("Stack overflow in ObjMap.toString(). This may be due to the mapped object having a getter that returns ObjMap(this). Make that a function instead.")
                    }
                    
            }
        }.toSet()

    override val values get() =
        clazz.props.map {
            it.value.get(obj)
        }

    override fun containsKey(key: String) = clazz.props.containsKey(key)

    override fun containsValue(value: Any?) =
        clazz.props.values.any {
            it.get(obj) == value
        }

    override fun get(key: String) = clazz.props[key]?.get(obj)
    override fun isEmpty() = clazz.props.isEmpty()

    override fun toString() =
        StringBuilder(size * 128).apply {
            append('{')
            var first = true
            entries.forEach { e ->
                if (first) {
                    first = false
                } else {
                    append(", ")
                }
                append(e.key)
                append('=')
                append(e.value)
            }
            append('}')
        }.toString()

    override fun equals(other: Any?) =
        when {
            other === this ->        true
            other is ObjMap<*> ->    other.obj == obj
            other is Map<*, *> ->    other.size == size && toMap() == other // TODO optimize?
            else ->                  false
        }

    override fun hashCode() =
        entries.sumOf { it.hashCode() }

}

/**
 * [ObjMap] wrapper for the receiver.
 */
val <T : Any> T.asMap: Map<String, Any?> get() = ObjMap(this)


/**
 * Caches the per-class information used by [ObjMap].
 */
private class ClassMapInfo<T : Any>(clazz: KClass<T>) {

    val props: Map<String, KProperty1<T, *>> =
            clazz.memberProperties
                .filter { it.visibility == KVisibility.PUBLIC }
                .associateBy { it.name }

    // implements MutableSet so ObjMap can implement MutableMap,
    // but is of course not actually mutable.
    val keys: MutableSet<String> =
            object : MutableSet<String>, Set<String> by props.keys {

                override fun iterator() =
                        object : MutableIterator<String>, Iterator<String> by props.keys.iterator() {
                            override fun remove() {
                                unsupported()
                            }
                        }

                override fun add(element: String): Boolean {
                    throw unsupported()
                }

                override fun addAll(elements: Collection<String>): Boolean {
                    throw unsupported()
                }

                override fun clear() {
                    throw unsupported()
                }

                override fun remove(element: String): Boolean {
                    throw unsupported()
                }

                override fun removeAll(elements: Collection<String>): Boolean {
                    throw unsupported()
                }

                override fun retainAll(elements: Collection<String>): Boolean {
                    throw unsupported()
                }

            }

    private fun unsupported() =
            UnsupportedOperationException("ObjMap doesn't support add/remove operations")

}

private val KClass<Any>.classMapInfoUntyped: ClassMapInfo<Any>
    by StoredExtensionProperty {
        ClassMapInfo(this())
    }
@Suppress("UNCHECKED_CAST")
private val <T : Any> KClass<T>.classMapInfo: ClassMapInfo<T> get() =
    (this as KClass<Any>).classMapInfoUntyped as ClassMapInfo<T>
