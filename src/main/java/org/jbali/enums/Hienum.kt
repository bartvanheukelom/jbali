package org.jbali.enums

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
abstract class Hienum<R : Hienum<R>>(
        /** The root class of this hierarchical enumeration, e.g. `Animalia::class`. */
        @Transient val rootClass: KClass<R>
) : Serializable, Comparable<R> {

    val kClass get() =
        javaClass.kotlin

    init {

        // TODO check parent is sealed (once per parent, so lazy)

        // require each leaf to be an object
        require(javaClass.declaredFields.singleOrNull {
            it.name == "INSTANCE" && it.type == javaClass && it.isStatic && it.isFinal
        } != null) {
            throw AssertionError("INSTANCE not found on $javaClass. It should be an object.")
        }

        // and require each leaf but also each parent class to declare serialVersionUID 1
        var jc: Class<*> = javaClass
        while (jc != Hienum::class.java) {
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

    /**
     * The name for this Hienum value, which equals the qualified class name with the qualified [rootClass] name prefix removed,
     * e.g. `Animalia.Carnivora.Felidae.FCatus.name == "Carnivora.Felidae.FCatus"`
     */
    @Transient
    val name =
            javaClass.canonicalName.removePrefix(rootClass.java.canonicalName + ".")

    override fun toString() =
            name

    // TODO optimize by assigning each value an index after init
    override fun compareTo(other: R) =
            name.compareTo(other.name)

    // Effects of java.io.Serializable on this object hierarchy:
    // - Deserialization will create temporary non-canonical instances of each class, but readResolve resolves that
    // - All fields must be transient, so the serialized form has no data.
    //   As a result, those fields are only inited in canonical instances, but that is the only place they are read anyway.
    // - Each class (including intermediates) must declare serialUID 1
    protected fun readResolve(): Any = javaClass.kotlin.objectInstance!!

//    /**
//     * Returns whether this hienum value is exactly [selector], or has [selector] as parent.
//     *
//     *     assertTrue(Animalia.Carnivora.Felidae.FCatus matches Animalia.Carnivora.Felidae.FCatus)
//     *     TODO the following do not work because objects cant inherit from each other
//     *     assertTrue(Animalia.Carnivora.Felidae.FCatus matches Animalia.Carnivora.Felidae)
//     *     assertFalse(Animalia.Carnivora.Felidae.FCatus matches Animalia.Carnivora.Canidae)
//     */
//    infix fun R.matches(selector: R): Boolean =
//            selector.javaClass.isInstance(this)

    abstract class HienumCompanion<R : Hienum<R>>(
            val rootClass: KClass<R>
    ) : StringBasedSerializer<R>(rootClass) {

        // must all be lazy because the leaf objects have not been constructed yet when this companion is inited
        // TODO group into 1 lazy, or explicitly init this as soon as possible

        val values by lazy {
            try {
                rootClass.loadSealedObjects()
            } catch (e: Exception) {
                throw AssertionError("Could not load sealed objects of $rootClass: $e", e)
            }
        }

        val allByName: Map<String, R> by lazy {
            values.associateBy { it.name }
        }

        override fun fromString(s: String): R =
                allByName.getValue(s)
        override fun toString(o: R) =
                o.name
    }

    companion object {

        /**
         * @throws NoSuchElementException
         */
        @JvmStatic
        fun <R : Hienum<R>> getByName(type: Class<R>, name: String): R =
                type.kotlin.allByName.getValue(name)

    }

}

@Suppress("UNCHECKED_CAST")
val <R : Hienum<R>> KClass<R>.companion: Hienum.HienumCompanion<R> get() =
        companionObjectInstance as Hienum.HienumCompanion<R>

val <R : Hienum<R>> KClass<R>.allByName: Map<String, R>
    get() = companion.allByName
