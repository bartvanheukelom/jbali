package org.jbali.kotser

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import org.jbali.json2.JSONString
import org.jbali.reflect.kClass
import org.jbali.util.stringToBeUnmarshalled
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput

/**
 * Contains the JSON representation of an object.
 * This container is [Externalizable] and can be passed through Java's serialization mechanism.
 * It does not contain type information, the receiver must know the type to deserialize to.
 */
class JsonSerialized<T : Any>
constructor(
        var json: JSONString
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
        out.writeObject(json.string)
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
