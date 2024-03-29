package org.jbali.serialize

import org.jbali.reflect.Proxies

import java.io.*
import java.lang.reflect.InaccessibleObjectException
import java.lang.reflect.Method

object JavaSerializer {

    @JvmStatic
    fun write(message: Serializable): ByteArray {
        try {
            val b = ByteArrayOutputStream()
            val out = ObjectOutputStream(b)
            out.writeObject(message)
            out.flush()
            return b.toByteArray()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    @JvmStatic
    fun read(data: ByteArray): Any {
        try {
            val b = ByteArrayInputStream(data)
            val `in` = ObjectInputStream(b)
            return `in`.readObject()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    inline fun <reified T : Any> copyingProxy(impl: T): T {
        require(T::class.java.isInterface)
        return Proxies.create(T::class.java) { method, args ->
            val ret = method.invoke(impl, *args.map { it?.let { copy(it as Serializable) } }.toTypedArray())
            ret?.let { copy(it as Serializable) }
        }
    }

    @JvmStatic
    fun verifySerializable(obj: Any, paramName: String) {
        if (obj !is Serializable) {
            throw IllegalArgumentException("$paramName implementation ${obj.javaClass.name} is not Serializable")
        }

        try {
            copy(obj)
        } catch (e: Throwable) {
            throw IllegalArgumentException("verifySerializable failed for $paramName: $e")
        }
    }

    private fun oscMethod(method: String): Lazy<Method> = lazy {
        try {
            ObjectStreamClass::class.java.getDeclaredMethod(method)
                .apply { isAccessible = true }
        } catch (e: InaccessibleObjectException) {
            throw IllegalStateException("This assertion requires JVM arguments: --add-opens java.base/java.io=ALL-UNNAMED", e)
        }
    }
    private val ObjectStreamClass_hasReadResolveMethod  by oscMethod("hasReadResolveMethod" )
    private val ObjectStreamClass_hasWriteReplaceMethod by oscMethod("hasWriteReplaceMethod")
    
    /**
     * Assert that the given class has a correct `readResolve` method for Java serialization.
     *
     * Such a method MUST:
     * - be named `readResolve`
     * - have JVM return type [Object], nothing more specific
     *
     * It MAY:
     * - be private
     * - omit declaring [ObjectStreamException]
     * - make the return type `@NotNull Object`, or [Any] in Kotlin
     *
     * This implementation delegates to a private method of [ObjectStreamClass],
     * and requires JVM arguments `--add-opens java.base/java.io=ALL-UNNAMED`.
     *
     * [https://docs.oracle.com/javase/7/docs/platform/serialization/spec/input.html#5903]
     */
    // TODO implement this check as an annotation processor - update: actually just use @Serial
    fun assertReadResolve(clazz: Class<*>): ObjectStreamClass {
        val osc = ObjectStreamClass.lookup(clazz)
        val hasReRe = ObjectStreamClass_hasReadResolveMethod.invoke(osc) as Boolean
        if (!hasReRe) {
            throw AssertionError("$clazz does not have a conformant readResolve method. See assertReadResolve method doc for requirements.")
        }
        return osc
    }

    fun assertWriteReplace(clazz: Class<*>): ObjectStreamClass {
        val osc = ObjectStreamClass.lookup(clazz)
        val hasReRe = ObjectStreamClass_hasWriteReplaceMethod.invoke(osc) as Boolean
        if (!hasReRe) {
            throw AssertionError("$clazz does not have a conformant writeReplace method")
        }
        return osc
    }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    /**
     * @return A copy of the given object created using Java serialization
     */
    fun <T : Serializable> copy(obj: T): T {
        return read(write(obj)) as T
    }

}
