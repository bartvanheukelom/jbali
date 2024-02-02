package org.jbali.util

import kotlin.reflect.KClass


/**
 * Return all objects in a sealed object hierarchy with root [T].
 * That is, a type hierarchy where every type is either an object or a sealed class.
 * @throws IllegalArgumentException if a type is not an object or a sealed class.
 */
inline fun <reified T : Any> loadSealedObjects(): Set<T> =
        T::class.loadSealedObjects()

/**
 * Return all objects in a sealed object hierarchy with root [T].
 * That is, a type hierarchy where every type is either an object or a sealed class.
 * @throws IllegalArgumentException if a type is not an object or a sealed class.
 */
fun <T : Any> KClass<out T>.loadSealedObjects(): Set<T> =
        ArrayList<T>().also {
            loadSealedObjects(this, it)
        }.toSet()

private fun <T : Any> loadSealedObjects(c: KClass<out T>, into: MutableCollection<T>) {
    try {
        if (c.isSealed) c.sealedSubclasses.forEach {
            loadSealedObjects(it, into)
        }
        else {
            try {
                into.add(c.objectInstance ?: throw IllegalArgumentException("non sealed, non object $c"))
            } catch (e: TypeCastException) {
                throw IllegalStateException("It appears $c.objectInstance is being accessed while that instance has not yet been constructed", e)
            }
        }
    } catch (e: Throwable) {
        throw IllegalArgumentException("Error in loadSealedObjects($c): $e", e)
    }
}

val <T : Any, B : T> KClass<B>.parentOrNull: KClass<out T>?
    get() = supertypes.firstOrNull {
        it.classifier is KClass<*>
    }?.classifier as? KClass<out T>

val <T : Any, B : T> KClass<B>.parent: KClass<out T>
    get() = parentOrNull ?: error("$this has no parent class. Supertypes: $supertypes")

val <T : Any, B : T> KClass<B>.outerParentOrNull: KClass<out T>?
    get() = parentOrNull?.takeIf {
        this.qualifiedName != null && it.qualifiedName != null &&
                this.qualifiedName!!.startsWith(it.qualifiedName!! + ".")
    }

val <T : Any, B : T> KClass<B>.outerParent: KClass<out T>
    get() = outerParentOrNull ?: error("$this's parent $parent is not the containing outer class")
