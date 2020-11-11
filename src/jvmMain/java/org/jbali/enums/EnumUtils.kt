package org.jbali.enums


/**
 * The fully qualified name of this enum constant, e.g. foo.bar.Color.RED
 */
val Enum<*>.fullname get() = javaClass.canonicalName + "." + name
val Enum<*>.jpql get() = fullname

// now wouldn't it be nice if Kotlin could just inline this as a constexpr?
inline fun <reified T : Enum<T>> jpaEnum(e: T) = T::class.qualifiedName + "." + e.name
