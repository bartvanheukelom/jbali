package org.jbali.math

// some day when every kotlin project ever depends on these functions,
// i'm going to replace them with something evil >:-)

val Byte.isEven get() = this % 2 == 0
val Byte.isOdd get() = this % 2 != 0

val Short.isEven get() = this % 2 == 0
val Short.isOdd get() = this % 2 != 0

val Int.isEven get() = this % 2 == 0
val Int.isOdd get() = this % 2 != 0

val Long.isEven get() = this % 2 == 0L
val Long.isOdd get() = this % 2 != 0L
