package org.jbali.hum

import org.jbali.reflect.kClass
import org.jbali.reflect.objectInstanceField
import org.jbali.serialize.SerializableObject
import kotlin.reflect.KClass

/**
 * Base class for hierarchical enumerations.
 */
abstract class HumValue<R : HumValue<R>>(
        /** The root class of this hierarchical enumeration, e.g. `Animalia::class`. */
        val root: HumRoot<R>
) :
        SerializableObject(),
        Comparable<R>
{

    private val rootClass: KClass<R> get() = root.rootClass

    init {

        // TODO check parent is sealed (once per parent, so lazy)

        // require each leaf to be an object
        require(kClass.objectInstanceField != null) {
            throw AssertionError("INSTANCE not found on $javaClass. It should be an object.")
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
    val name =
            kClass.qualifiedName!!.removePrefix(rootClass.qualifiedName!! + ".")

    override fun toString() =
            name

    override fun compareTo(other: R) =
            ordinal.compareTo(other.ordinal)

    // TODO is this required?
    @Suppress("UNCHECKED_CAST")
    inline val typedThis get() = this as R

    companion object {

        @JvmStatic
        fun <R : HumValue<R>> getGroup(type: Class<R>): HumGroup<R> =
                type.kotlin.humGroup

    }

}
