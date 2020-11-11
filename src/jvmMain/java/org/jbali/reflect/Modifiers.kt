package org.jbali.reflect

import java.lang.reflect.Member
import java.lang.reflect.Modifier

inline class Modifiers(
        val m: Int
) {
    val isPublic       get() = Modifier.isPublic(m)
    val isPrivate      get() = Modifier.isPrivate(m)
    val isProtected    get() = Modifier.isProtected(m)
    val isStatic       get() = Modifier.isStatic(m)
    val isFinal        get() = Modifier.isFinal(m)
    val isSynchronized get() = Modifier.isSynchronized(m)
    val isVolatile     get() = Modifier.isVolatile(m)
    val isTransient    get() = Modifier.isTransient(m)
    val isNative       get() = Modifier.isNative(m)
    val isInterface    get() = Modifier.isInterface(m)
    val isAbstract     get() = Modifier.isAbstract(m)
    val isStrict       get() = Modifier.isStrict(m)
}

val Class<*>.classModifiers get() = Modifiers(modifiers)
val Member.memberModifiers get() = Modifiers(modifiers)

// TODO add the others
val Member.isStatic get() = memberModifiers.isStatic
val Member.isFinal get() = memberModifiers.isFinal
