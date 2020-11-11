package org.jbali.util

import kotlinx.serialization.Serializable
import org.jbali.kotser.PartialObjectSerializer
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Represents a partial set of properties of [T] and their values.
 * [T] can be an interface that has no implementation.
 */
@Serializable(with = PartialObjectSerializer::class)
sealed class PartialObject<T> {
    // TODO implement Map, remove toMap, make inline get/getValue extensions

    abstract val properties: Set<KProperty1<T, *>>

//    class Complete<T>(val obj: T)

    abstract fun toMap(): Map<KProperty1<T, *>, Any?>

    abstract fun doGet(prop: KProperty1<T, *>): Any?
    abstract fun doRequire(prop: KProperty1<T, *>): Any?

    inline operator fun <reified R> get(prop: KProperty1<T, R>): R? =
            doGet(prop) as R?

    inline fun <reified R> getValue(prop: KProperty1<T, R>): R =
            doRequire(prop) as R

    // TODO type name
    final override fun toString() = properties.joinToString(
            prefix = "Partial<...>(",
            postfix = ")"
    ) {
        "${it.name}=${getValue(it)}"
    }

    final override fun equals(other: Any?): Boolean =
            when (other) {
                is PartialObject<*> -> toMap() == other.toMap()
                else -> false
            }

    // TODO optimize
    final override fun hashCode() =
            toMap().hashCode()

    class Mapped<T>(
            val props: Map<KProperty1<T, *>, Any?>
    ) : PartialObject<T>() {

        override fun toMap(): Map<KProperty1<T, *>, Any?> =
                props

        override val properties: Set<KProperty1<T, *>>
            get() = props.keys

        override fun doGet(prop: KProperty1<T, *>): Any? =
                props[prop]

        override fun doRequire(prop: KProperty1<T, *>): Any? =
                props.getValue(prop)


    }
}

inline fun <reified T : Any> T.partial(
        include: (KProperty1<T, *>) -> Boolean = { true }
): PartialObject<T> =
        PartialObject.Mapped(T::class.memberProperties
                .filter(include)
                .associateWith { it.get(this) })

