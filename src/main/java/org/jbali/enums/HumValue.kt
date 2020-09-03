package org.jbali.enums

import org.jbali.collect.ListSet
import org.jbali.collect.toListSet
import org.jbali.kotser.StringBasedSerializer
import org.jbali.reflect.isFinal
import org.jbali.reflect.isStatic
import org.jbali.util.loadSealedObjects
import java.io.ObjectStreamClass
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance


/**
 * Base class for hierarchical enumerations.
 */
abstract class HumValue<R : HumValue<R>>(
        /** The root class of this hierarchical enumeration, e.g. `Animalia::class`. */
        @Transient val root: HumRoot<R>
) : Serializable, Comparable<R> {

    private val rootClass: KClass<R> get() = root.rootClass

    val kClass get() =
        javaClass.kotlin

    init {

//        println("init value $javaClass")

        // TODO check parent is sealed (once per parent, so lazy)

        // require each leaf to be an object
        require(javaClass.declaredFields.singleOrNull {
            it.name == "INSTANCE" && it.type == javaClass && it.isStatic && it.isFinal
        } != null) {
            throw AssertionError("INSTANCE not found on $javaClass. It should be an object.")
        }

        // and require each leaf but also each parent class to declare serialVersionUID 1
        var jc: Class<*> = javaClass
        while (jc != HumValue::class.java) {
            ObjectStreamClass.lookup(jc).let { osc ->
                require(osc.serialVersionUID == 1L) {
                    throw AssertionError("$jc must declare: const val serialVersionUID = 1L")
                }
                require(osc.fields.isEmpty()) {
                    throw AssertionError("$jc must not have any non-transient fields: ${osc.fields.joinToString { 
                        "$it"
                    }}")
                }
            }
            jc = jc.superclass ?: throw AssertionError()
        }
    }

    val ordinal: Int by lazy {
        root.values.indexOf(this).also {
            if (it < 0) {
                throw AssertionError("$this !in ${root.values}")
            }
        }
    }

    /**
     * The name for this [HumValue], which equals the qualified class name with the qualified [rootClass] name prefix removed,
     * e.g. `Animalia.Carnivora.Felidae.FCatus.name == "Carnivora.Felidae.FCatus"`
     */
    @Transient
    val name =
            javaClass.canonicalName.removePrefix(rootClass.java.canonicalName + ".")

    override fun toString() =
            name

    override fun compareTo(other: R) =
            ordinal.compareTo(other.ordinal)

    // Effects of java.io.Serializable on this object hierarchy:
    // - Deserialization will create temporary non-canonical instances of each class, but readResolve resolves that
    // - All fields must be transient, so the serialized form has no data.
    //   As a result, those fields are only inited in canonical instances, but that is the only place they are read anyway.
    // - Each class (including intermediates) must declare serialUID 1
    protected fun readResolve(): Any = javaClass.kotlin.objectInstance!!

//    init {
//        println("mid init value $javaClass")
//    }

    companion object {

        @JvmStatic
        fun <R : HumValue<R>> getGroup(type: Class<R>): HumGroup<R> =
                type.kotlin.humGroup

    }

//    init {
//        println("end init value $javaClass")
//    }

}

// TODO also companion for values?
abstract class HumGroup<R : HumValue<*>>(
        val groupClass: KClass<R>
) : StringBasedSerializer<R>(groupClass) {

//    init {
//        println("init $this")
//    }

    override fun toString() =
            "HumGroup(${groupClass.qualifiedName})"

    /**
     * Note that you would normally use:
     * - `hv is Animalia.Carnivora`
     * instead of:
     * - `hv in Animalia.Carnivora`
     * but this allows something like:
     * - `hv in dynamicGroup`
     */
    operator fun contains(hv: HumValue<*>) =
        groupClass.isInstance(hv)

    // TODO find a way to init this once per hierarchy, instead of the runtime overhead of lazy
    // must be lazy because the leaf objects have not been constructed yet when this companion is inited
    private val late by lazy { Late() }; inner class Late {

//        println("Late init ${this@HienumGroupCompanion}")
//        Exception().printStackTrace()

        val values: ListSet<R> =
                try {
                    groupClass.loadSealedObjects().toListSet()
                } catch (e: Exception) {
                    throw AssertionError("Could not load sealed objects of $groupClass: $e", e)
                }

        val allByName: Map<String, R> =
            values.associateBy { it.name }

    }

    val values: ListSet<R> get() = late.values
    val byName: Map<String, R> get() = late.allByName

    override fun fromString(s: String): R =
            byName.getValue(s)
    override fun toString(o: R) =
            o.name


//    init {
//        println("end init group $javaClass")
//    }
}

abstract class HumRoot<R : HumValue<R>>(
        rootClass: KClass<R>
) : HumGroup<R>(rootClass) {

    val rootClass get() = groupClass

//    fun init() {
//        Exception("INIT").printStackTrace()
//        values
//    }

    override fun toString() =
            "HumRoot(${groupClass.qualifiedName})"

}

val <R : HumValue<R>> KClass<R>.humGroup: HumGroup<R> get() {
    val grp = companionObjectInstance as HumGroup<*>
    if (grp.groupClass != this) {
        throw AssertionError()
    }
    @Suppress("UNCHECKED_CAST")
    return grp as HumGroup<R>
}
