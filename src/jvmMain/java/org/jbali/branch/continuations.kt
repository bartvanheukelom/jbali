@file:Suppress("UNCHECKED_CAST")

package org.jbali.branch

import org.jbali.bytes.theUnsafe
import kotlin.coroutines.Continuation

object SafeCont {
    val clazz = Class.forName("kotlin.coroutines.SafeContinuation")
    val f_delegate = clazz.declaredFields.singleOrNull {
        it.name == "delegate"
    }
        .let { it
            ?: error("SafeContinuation must have a single delegate field")
        }
        .apply { isAccessible = true }
}

data class ContinuationClass(
    val type: Class<*>,
) {
    
    val fields by lazy { buildList {
        var c = type
        while (c != Any::class.java) {
            c.declaredFields.forEach {
                it.isAccessible = true
                add(it)
            }
            c = c.superclass
        }
    } }
    
    val invokeSuspend by lazy {
        type.methods.singleOrNull {
            it.name == "invokeSuspend" && it.parameterCount == 1
            && it.parameterTypes[0] == Object::class.java
        }
            .let { it
                ?: error("Missing $type invokeSuspend(Object)")
            }
    }
    
    fun clone(cont: Continuation<*>): Continuation<*> {
        val clone = theUnsafe.allocateInstance(type) as Continuation<Any?>
        fields.forEach { it.set(clone, it.get(cont)) }
        return clone
    }
    
}