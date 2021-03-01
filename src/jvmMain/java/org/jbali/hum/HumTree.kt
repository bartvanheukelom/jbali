package org.jbali.hum

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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


/**
 * Some subtree of a hierarchical enum.
 * @param G all values in the subtree are of this type, but not all possible values of this type may be in the subtree.
 */
interface HumTreeOut<out G> : ListSet<G> {

    @Deprecated("use direct")
    val values: ListSet<G>

//    /**
//     * Note that you would normally use:
//     * - `hv is Animalia.Carnivora`
//     * instead of:
//     * - `hv in Animalia.Carnivora`
//     * but this allows something like:
//     * - `hv in dynamicGroup`
//     */
//    operator fun contains(element: @UnsafeVariance G): Boolean

}


/**
 * Represent a single subtree of a hierarchical enum, which includes the option of representing a single value,
 * or the entire tree.
 *
 * Every _object_ in a hierarchical enum definition is a subclass of [HumTree]. That is, every value object, and every (sealed) group companion.
 *
 * @param G all values in the subtree are of this type AND all possible values of this type are in the subtree.
 */
sealed class HumTree<R : HumValue<R>, G : R>(
        val rootClass: KClass<R>,
        gc: KClass<G>?
) :
        HumTreeOut<R>, // TODO allow G
        KSerializer<G>,
        // TODO KSerializer<HumGroup>
        Externalizable
        // TODO ListSet<G>,
{

    @Suppress("UNCHECKED_CAST")
    val groupClass: KClass<G> = gc ?: kClass as KClass<G>

    init {
        require(groupClass.isSubclassOf(rootClass)) {
            "$groupClass is not a subclass of $rootClass"
        }
        require(kClass.isObject) {
            "$kClass should be a (companion) object"
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
            byName[s] ?: throw NoSuchElementException("$this has no value named '$s'. Did you mean '${closestMatch(s)}'?")

    fun closestMatch(s: String): G =
        values.minByOrNull { nameDistance(it.name, s) }
            ?: throw NoSuchElementException("$this has no values?")

    // TODO levenshtein
    private fun nameDistance(canonical: String, s: String): Int =
        when {
            s == canonical -> 0
            s.trim().equals(canonical, ignoreCase = true) -> 1
            else -> 100
        }

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

    override val descriptor get() = ser.descriptor

    override fun deserialize(decoder: Decoder): G = ser.deserialize(decoder)
    override fun serialize(encoder: Encoder, value: G) = ser.serialize(encoder, value)

    private fun containsImpl(element: Any?) =
            groupClass.isInstance(element)

    override fun contains(element: R) =
            containsImpl(element)

    override fun containsAll(elements: Collection<R>): Boolean =
            when (elements) {
                is HumTree<R, *> -> contains(elements)
                is HumValue<R> -> containsImpl(elements)
                else -> elements.all { it in this }
            }

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

    override val size: Int
        get() = late.values.size

    override fun get(index: Int): R = late.values.get(index)
    override fun indexOf(element: R): Int = late.values.indexOf(element)
    override fun isEmpty(): Boolean = late.values.isEmpty()
    override fun iterator(): Iterator<R> = late.values.iterator()
    override fun lastIndexOf(element: R): Int = late.values.lastIndexOf(element)
    override fun listIterator(): ListIterator<R> = late.values.listIterator()
    override fun listIterator(index: Int): ListIterator<R> = late.values.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<R> = late.values.subList(fromIndex, toIndex)

    //    fun <G, V> associateWith(valueGetter: (G) -> V): Map<G, V> =
//            HumMap<G, V>(
//                    type = this,
//                    values = values.map(valueGetter)
//            )

}

/**
 * Base class for companions of enum root or subdivision.
 */
sealed class HumGroup<R : HumValue<R>, G : R>(
        rootClass: KClass<R>,
        groupClass: KClass<G>
)
    : HumTree<R, G>(rootClass, groupClass) {

    init {
        require(groupClass.isSealed) {
            "$groupClass is not sealed"
        }
    }

}



// ==================================== base classes for implementations ================================= //


/**
 * Base class for hierarchical enumeration root companions.
 */
abstract class HumRoot<R : HumValue<R>>(rootClass: KClass<R>)
    : HumGroup<R, R>(rootClass, rootClass)


/**
 * Base class for hierarchical enumeration subgroup companions.
 */
abstract class HumBranch<R : HumValue<R>, G : R>(
        rootClass: KClass<R>,
        groupClass: KClass<G>
)
    : HumGroup<R, G>(rootClass, groupClass)


/**
 * Base class for hierarchical enumeration values.
 */
abstract class HumValue<R : HumValue<R>>(
        /** The root class of this hierarchical enumeration, e.g. `Animalia::class`. */
        val root: HumRoot<R>
) :
        HumTree<R, R>(root.rootClass), // TODO G
        Comparable<R>
{

    // optimization (I think) that's functionally the same
    override operator fun contains(element: R): Boolean =
            element == this

    val ordinal: Int by lazy {
        root.indexOf(this).also {
            if (it < 0) {
                throw AssertionError("$this !in ${root.toSet()}")
            }
        }
    }

    override fun compareTo(other: R) =
            ordinal.compareTo(other.ordinal)

    companion object {

        @JvmStatic
        fun getGroup(type: Class<*>): HumTree<*, *> =
                type.kotlin.forceHumGroup

    }

}



// ========================= dynamic shenanigans ========================= //

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
