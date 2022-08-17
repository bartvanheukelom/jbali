package org.jbali.reflect

import java.lang.reflect.Field
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
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

/**
 * Whether this is the class of a Kotlin (companion) _object_. In the latter case, uses a heuristic that may give false positives.
 *
 * Unlike [KClass.objectInstance], this can be called before or while the class or object are being initialized.
 */
val <T : Any> KClass<T>.isObject: Boolean get() =
        isCompanion || objectInstanceField != null

/**
 * The static [Field] of this _object_ class that refers to the singular instance, if any.
 *
 * Unlike [KClass.objectInstance], this can be called before or while the class or object are being initialized.
 */
val <T : Any> KClass<T>.objectInstanceField: Field? get() =
    javaObjectType.declaredFields.singleOrNull {
        it.name == "INSTANCE" && it.type == javaObjectType && it.isStatic && it.isFinal
    }

/**
 * Returns the runtime [KClass] of this object.
 */
val <T : Any> T.kClass: KClass<T> get() =
        javaClass.kotlin

/**
 * Returns the runtime [KClass] of this object.
 */
val <T : Any> T?.kClassOrNull: KClass<T>? get() =
    this?.javaClass?.kotlin

val Class<*>.binaryName: String get() {
    val pp = `package`.name + "."
    return canonicalName.removePrefix(pp).replace('.', '$')
}

val Class<*>.qualifiedBinaryName: String get() =
        "${`package`.name}.$binaryName"


/**
 * Normally equal to [KCallable.callBy], but tries to throw more helpful exceptions if:
 * - the type of an argument does not match the type of the parameter
 */
fun <R> KCallable<R>.callByWithBetterExceptions(args: Map<KParameter, Any?>): R =
    try {
        callBy(args)
    } catch (iae: IllegalArgumentException) {
        if (iae.message == "argument type mismatch") {
            
            // this condition was already detected, hence we got iae, but unfortunately it's message isn't so useful, so reconstruct it
            parameters.forEach { param ->
                (param.type.classifier as? KClass<*>)?.let { paramClass ->
                    val arg = args[param]
                    if (!paramClass.isInstance(arg)) {
                        throw IllegalArgumentException("Argument to\n    $param\nis of invalid type\n    ${arg?.javaClass}")
                    }
                }
            }
            
        }
        val argsNameType: Map<String, String?> =
            args.entries.associate { (k, v) ->
                k.name!! to v.kClassOrNull?.qualifiedName
            }
        throw IllegalArgumentException("($this)\n\t.callBy($argsNameType): $iae", iae)
    }
