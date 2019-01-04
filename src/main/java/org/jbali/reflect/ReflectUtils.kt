package org.jbali.reflect

import java.lang.reflect.Field

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
