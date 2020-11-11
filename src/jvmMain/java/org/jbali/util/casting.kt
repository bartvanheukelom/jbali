package org.jbali.util

import org.jbali.reflect.kClass
import kotlin.reflect.KClass
import kotlin.reflect.full.cast


/**
 * Casts the receiver to the class represented by [to].
 * @throws TypeCastException if the receiver is `null` or if it is not an instance of [to].
 */
infix fun <A, B : Any> A.asClass(to: KClass<B>): B =
        when (this) {
            null -> throw TypeCastException("null cannot be cast to ${to.qualifiedName}")
            else ->
                try {
                    to.cast(this)
                } catch (e: TypeCastException) {
                    throw TypeCastException("Value of type ${(this as Any).kClass.qualifiedName} cannot be cast to ${to.qualifiedName}")
                }
        }


inline fun <reified T : Any> Any.cast() = this as T
