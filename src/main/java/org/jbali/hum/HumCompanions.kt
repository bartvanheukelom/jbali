package org.jbali.hum

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import org.jbali.collect.ListSet
import org.jbali.collect.toListSet
import org.jbali.kotser.StringBasedSerializer
import org.jbali.reflect.isObject
import org.jbali.reflect.kClass
import org.jbali.serialize.SerializedObject
import org.jbali.util.loadSealedObjects
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import java.io.ObjectStreamException
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubclassOf

abstract class HumRoot<R : HumValue<R>>(rootClass: KClass<R>)
    : HumTree<R, R>(rootClass, rootClass)

interface HumTreeOut<out G> {
    val values: ListSet<G>
}

/**
 * Represent a single subtree of a hierarchical enum, which includes the option of representing a single value,
 * or the entire tree.
 */
abstract class HumTree<R : HumValue<R>, G : R>(
        val rootClass: KClass<R>,
        gc: KClass<G>?
) :
        HumTreeOut<G>,
        KSerializer<G>,
        // TODO KSerializer<HumGroup>
        Externalizable
        // TODO ListSet<G>,
{

    @Suppress("UNCHECKED_CAST")
    val groupClass: KClass<G> = gc ?: kClass as KClass<G>

    init {
        require(groupClass.isSubclassOf(rootClass))
        require(kClass.isObject) {
            throw AssertionError("$kClass should be a (companion) object")
        }
    }

    private val ser: StringBasedSerializer<G> = object : StringBasedSerializer<G>(groupClass) {
        override fun fromString(s: String): G = this@HumTree.fromString(s)
        override fun toString(o: G) = this@HumTree.toString(o)
    }

    // for use by HumValue
    constructor(rootClass: KClass<R>) : this(rootClass, null)

    /**
     * The name for this [HumValue], which equals the qualified class name with the qualified [rootClass] name prefix removed,
     * e.g. `Animalia.Carnivora.Felidae.FCatus.name == "Carnivora.Felidae.FCatus"`
     */
    val name =
            if (groupClass == rootClass) {
                ""
            } else {
                groupClass.qualifiedName!!.removePrefix(rootClass.qualifiedName!! + ".")
            }

    final override fun toString() =
            name

    fun fromString(s: String): G =
            byName.getValue(s)

    fun toString(o: G) =
            o.name

    @Throws(ObjectStreamException::class)
    protected fun writeReplace(): Any =
            SerializedObject(javaClass.name)

    override fun readExternal(`in`: ObjectInput?) {
        throw AssertionError("should have been writeReplace'd")
    }

    override fun writeExternal(out: ObjectOutput?) {
        throw AssertionError("should have been writeReplace'd")
    }

    override val descriptor: SerialDescriptor get() = ser.descriptor
    override fun deserialize(decoder: Decoder): G = ser.deserialize(decoder)
    override fun serialize(encoder: Encoder, value: G) = ser.serialize(encoder, value)

    /**
     * Note that you would normally use:
     * - `hv is Animalia.Carnivora`
     * instead of:
     * - `hv in Animalia.Carnivora`
     * but this allows something like:
     * - `hv in dynamicGroup`
     */
    open operator fun contains(element: R): Boolean =
            groupClass.isInstance(element)

    operator fun contains(tree: HumTree<R, *>): Boolean =
            tree.groupClass.isSubclassOf(groupClass)

    // TODO find a way to init this once per hierarchy, instead of the runtime overhead of lazy
    // must be lazy because the leaf objects have not been constructed yet when this companion is inited
    private val late by lazy { Late() }; inner class Late {

        val values: ListSet<G> =
                try {
                    groupClass.loadSealedObjects().toListSet()
                } catch (e: Exception) {
                    throw AssertionError("Could not load sealed objects of $groupClass: $e", e)
                }

        // TODO group-local name (and ordinal)
        val allByName: Map<String, G> =
                values.associateBy { it.name }

    }

    override val values: ListSet<G> get() = late.values
    val byName: Map<String, G> get() = late.allByName

//    fun <G, V> associateWith(valueGetter: (G) -> V): Map<G, V> =
//            HumMap<G, V>(
//                    type = this,
//                    values = values.map(valueGetter)
//            )

}

val <R : HumValue<R>> KClass<R>.humRoot: HumRoot<R>
    get() {
        val grp = companionObjectInstance as HumRoot<*>
        if (grp.groupClass != this) {
            throw AssertionError()
        }
        @Suppress("UNCHECKED_CAST")
        return grp as HumRoot<R>
    }

val KClass<*>.forceHumGroup: HumTree<*, *> get() =
        companionObjectInstance as HumTree<*, *>

val <R : HumValue<R>, G : R> KClass<G>.humGroup: HumTree<R, G>
    get() {
        val grp = forceHumGroup
        if (grp.groupClass != this) {
            throw AssertionError()
        }
        @Suppress("UNCHECKED_CAST")
        return grp as HumTree<R, G>
    }
