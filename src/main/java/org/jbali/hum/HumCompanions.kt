package org.jbali.hum

import org.jbali.collect.ListSet
import org.jbali.collect.toListSet
import org.jbali.kotser.StringBasedSerializer
import org.jbali.util.loadSealedObjects
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

abstract class HumRoot<R : HumValue<R>>(
        rootClass: KClass<R>
) : HumGroup<R>(rootClass) {

    val rootClass get() = groupClass

    override fun toString() =
            "HumRoot(${groupClass.qualifiedName})"

}

// TODO also companion for values?
abstract class HumGroup<R : HumValue<*>>(
        val groupClass: KClass<R>
) : StringBasedSerializer<R>(groupClass) {

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

val <R : HumValue<R>> KClass<R>.humGroup: HumGroup<R>
    get() {
        val grp = companionObjectInstance as HumGroup<*>
        if (grp.groupClass != this) {
            throw AssertionError()
        }
        @Suppress("UNCHECKED_CAST")
        return grp as HumGroup<R>
    }
