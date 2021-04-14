package org.jbali.kt

/**
 * Use to check the type of an expresion at compile-time only:
 *
 * ```
 * // ensure we're calling the Long overload, generates a compile error
 * // if giveNumber() is changed to return e.g. Int
 * funWithOverloads(giveNumber().being { a: Long -> a }
 * ```
 *
 * The lambda [l] is not called, and only
 * exists because it can be used to declare the type in a way where IntelliJ
 * doesn't see that it's always true, as it would with `giveNumber() as Long`
 * or something like `giveNumber().beingA<Long>()`.
 * Likewise, the only reason the lambda should "return" its argument is so that
 * no warning about an usused parameter would be given.
 *
 * @return the receiver, unmodified (the whole call is actually inlined away)
 */
@Suppress("UNUSED_PARAMETER")
inline fun <T> T.being(l: (T) -> T): T = this



@Suppress("unused", "unused_variable", "useless_cast")
private fun beingCompileTimeTest() {

    @Suppress("ConstantConditionIf", "SimplifyBooleanWithConstants")
    val maybeLong = if (1 > 2) 12L else null

    val longOne = 1L

    // should give errors if you change longOne to Int:
    val c1: Long = longOne.being { a: Long -> a }
    val c2: Number = longOne.being { a: Long -> a }
    val c6: Long? = longOne.being { a: Long? -> a }

    // should give errors if you change longOne to String:
    val c3: Number = longOne.being { a: Number -> a }
    val c4: Long? = maybeLong.being { a: Long? -> a }
    val c5: Long? = null.being { a: Long? -> a }
    val c7: Number? = maybeLong.being { a: Long? -> a }



    // should give errors if uncommented:
//    val e1: Long = 1.being { a: Long -> a }
//    val e2: Long = maybeLong.being { a: Long -> a }
//    val e3: Long = maybeLong.being { a: Number -> a }
//    val e4: Long = (1 as Number).being { a: Long -> a }
//
//    inline fun mustBeThisTall(v: Number): Nothing = throw AssertionError()
//    inline fun mustBeThisTall(v: Long) {}
//    mustBeThisTall(1.being { a: Long -> a })

    // note how this is a false positive:
    val p4: Long = (1 as Number) as Long
}
