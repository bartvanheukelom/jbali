package org.jbali.reflect

import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Contains a JVM class field ref and an instance of that class,
 * and provides a simple getter/setter for the field's value.
 */
data class BoundField(
        val obj: Any,
        val fyld: Field
) {
    init {
        // checks if the field is accessible and the object has it (belongs to the proper class)
        try {
            fyld.get(obj)
        } catch (e: Throwable) {
            throw IllegalArgumentException("$fyld is not gettable from $obj for self-test: $e", e)
        }
    }

    var value: Any
        get() = fyld.get(obj)
        set(v) = fyld.set(obj, v)
}


/**
 * Throws [IllegalArgumentException] if [x] is not assignable to a parameter, variable or property of this [KType].
 *
 * However, due to type erasure, not throwing does not guarantee that [x] is in fact
 * a completely valid value of [KType].
 *
 * Specifically, this function only throws if:
 * - [x] is `null` and the type is not nullable
 * - the classifier is a class and [x] is not an instance of that class.
 *
 * @throws IllegalArgumentException see above
 */
fun KType.checkAssignableFrom(x: Any?) {

    if (x == null) {
        require(isMarkedNullable) {
            "null is not assignable to $this"
        }
    } else if (classifier is KClass<*>) {
        require((classifier as KClass<*>).isInstance(x)) {
            "Value of type ${x.javaClass} is not assignable to $this"
        }
    }
}


