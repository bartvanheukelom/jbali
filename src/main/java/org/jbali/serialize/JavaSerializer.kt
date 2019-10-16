package org.jbali.serialize

import org.jbali.reflect.Proxies

import java.io.*
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

    private val ObjectStreamClass_hasReadResolveMethod: Method =
            ObjectStreamClass::class.java.getDeclaredMethod("hasReadResolveMethod")
                .apply { isAccessible = true }

    fun assertReadResolve(clazz: Class<*>): ObjectStreamClass {
        val osc = ObjectStreamClass.lookup(clazz)
        val hasReRe = ObjectStreamClass_hasReadResolveMethod.invoke(osc) as Boolean
        if (!hasReRe) {
            throw AssertionError("$clazz does not have a correct readResolve method")
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