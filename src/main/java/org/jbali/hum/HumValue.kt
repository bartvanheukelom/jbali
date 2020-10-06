package org.jbali.hum

/**
 * Base class for hierarchical enumerations.
 */
abstract class HumValue<R : HumValue<R>>(
        /** The root class of this hierarchical enumeration, e.g. `Animalia::class`. */
        val root: HumRoot<R>
) :
        HumTree<R, R>(root.rootClass), // TODO G
//        SerializableObject(),
        Comparable<R>
{

    // optimization (I think) that's functionally the same
    override operator fun contains(element: R): Boolean =
            element == this

    val ordinal: Int by lazy {
        root.values.indexOf(this).also {
            if (it < 0) {
                throw AssertionError("$this !in ${root.values}")
            }
        }
    }

    override fun compareTo(other: R) =
            ordinal.compareTo(other.ordinal)

    // TODO is this required?
    @Suppress("UNCHECKED_CAST")
    inline val typedThis get() = this as R

    companion object {

        @JvmStatic
        fun getGroup(type: Class<*>): HumTree<*, *> =
                type.kotlin.forceHumGroup

    }

}
