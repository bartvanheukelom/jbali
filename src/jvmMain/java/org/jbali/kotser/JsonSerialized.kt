package org.jbali.kotser

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.jbali.json2.JSONString
import org.jbali.reflect.kClass
import org.jbali.util.stringToBeUnmarshalled
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import java.io.Serial
import kotlin.reflect.KClass

/**
 * Contains the JSON representation of an object.
 * This container is [Externalizable] and can be passed through Java's serialization mechanism.
 * It does not contain type information, the receiver must know the type to deserialize to.
 */
open class JsonSerialized<T : Any>
constructor(
    open var json: JSONString
) : Externalizable {

    internal constructor() : this(JSONString(stringToBeUnmarshalled))

    override fun toString() =
            json.toString()

    companion object {
        const val serialVersionUID = 1L

        val format = DefaultJson.indented

        inline fun <reified T : Any> wrap(obj: T): JsonSerialized<T> =
            JsonSerialized(JSONString.stringify(format, serializer(), obj))
    
        /**
         * Serialize the given object using the serializer of its runtime class.
         * Only works for non-generic types. For Java use.
         */
        @OptIn(InternalSerializationApi::class)
        @JvmStatic
        fun <T : Any> wrapConcrete(obj: T): JsonSerialized<T> =
            JsonSerialized(JSONString.stringify(format, obj.kClass.serializer(), obj))
    }

    inline fun <reified R : T> unwrap(): R =
            json.parse(format, serializer())

    override fun writeExternal(out: ObjectOutput) {
        out.write(1)
        out.writeObject(json.string) // should have been writeUTF, but whatevs
    }

    override fun readExternal(inp: ObjectInput) {
        val version = inp.read()
        require(version == 1) {
            "Cannot read version $version"
        }
        json = JSONString(inp.readObject() as String)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JsonSerialized<*>

        if (json != other.json) return false

        return true
    }

    override fun hashCode(): Int {
        return json.hashCode()
    }

}


/**
 * Contains the qualified class name and JSON representation of an object.
 * This container is [Externalizable] and can be passed through Java's serialization mechanism.
 */
data class TaggedJsonSerialized<T : Any>
constructor(
    var className: String,
    var json: JSONString,
) : Externalizable {
    
    internal constructor() : this(stringToBeUnmarshalled, JSONString(stringToBeUnmarshalled))
    
    override fun toString() = "$className:$json"
    
    companion object {
        const val serialVersionUID = 1L
    }
    
    override fun writeExternal(out: ObjectOutput) {
        out.write(1)
        out.writeUTF(className)
        out.writeUTF(json.string)
    }
    
    override fun readExternal(inp: ObjectInput) {
        val version = inp.read()
        require(version == 1) {
            "Cannot read version $version"
        }
        className = inp.readUTF()
        json = JSONString(inp.readUTF())
    }
    
}

abstract class ResolvableJsonSerializedBase<T : Any>(
    json: JSONString
) : JsonSerialized<T>(json) {
    
    protected abstract val jsonSerializer: JsonSerializer<T>
    protected open val className get() = jsonSerializer.serializer.descriptor.serialName
    
    override fun toString(): String = "JsonSerialized<$className>($json)"
    
    @Serial fun readResolve(): T = trueReadResolve()
    
    /**
     * Exists only so it can be overridden, which the original [readResolve] apparently can't (at least, trying that caused a test to fail).
     */
    protected open fun trueReadResolve(): T  = jsonSerializer.parse(json)
    
    @OptIn(InternalSerializationApi::class)
    abstract class CompanionBase<T : Any>(
        clazz: KClass<T>
    ) {
        val classJsonSerializer = jsonSerializer<T>(serializer = clazz.serializer())
        
        // is lazy so that the reflection bit only happens if it's actually used
        // TODO add elegant way to test/define this at start time instead of when first used
        val writeReplace: (T) -> Any by lazy {
            Class.forName(javaClass.name.removeSuffix("\$Companion"))
                // TODO use empty constructor since it must exists anyway, then assign json
                .getDeclaredConstructor(String::class.java)
                .apply { isAccessible = true }
                .let { c -> { c.newInstance(classJsonSerializer.stringify(it).string) } }
        }
    }
    
}


//@OptIn(InternalSerializationApi::class)
//abstract class JsonExternalizer : Externalizable {
//    
//    init {
//        require(javaClass.canonicalName != null) {
//            "$javaClass has no canonical name"
//        }
//        require(javaClass.typeParameters.isEmpty()) {
//            "$javaClass has type parameters"
//        }
//    }
//    
//    private val ser = jsonSerializer(
//        format = DefaultJson.plainOmitDefaults,
//        serializer = javaClass.kotlin.serializer(),
//    )
//    
//    fun write
//    
//    override fun writeExternal(out: ObjectOutput) {
//        out.write(1) // version
//        out.writeUTF(javaClass.canonicalName)
//        out.writeUTF(ser.stringify(this).string)
//    }
//    
//    override fun readExternal(inp: ObjectInput) {
//        val version = inp.read()
//        require(version == 1) {
//            "Cannot read version $version"
//        }
//        json = JSONString(inp.readObject() as String)
//    }
//    
//}
