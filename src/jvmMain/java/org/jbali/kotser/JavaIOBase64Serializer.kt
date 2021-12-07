package org.jbali.kotser

import org.jbali.serialize.JavaSerializer
import org.jbali.util.asClass
import java.util.*
import kotlin.reflect.KClass

/**
 * Serializes values of type [T] using [java.io.Serializable],
 * using [Base64] to encode the resulting blob.
 *
 * Extend to use for your type:
 *
 *     object YoshiSerializer : JavaIOBase64Serializer(Yoshi::class)
 *
 */
abstract class JavaIOBase64Serializer<T : java.io.Serializable>(
    private val clazz: KClass<T>,
) : StringBasedSerializer<T>(clazz) {
    
    override fun fromString(s: String): T =
        JavaSerializer.read(Base64.getDecoder().decode(s)) asClass clazz
    
    override fun toString(o: T): String =
        Base64.getEncoder().encodeToString(JavaSerializer.write(o))
    
}
