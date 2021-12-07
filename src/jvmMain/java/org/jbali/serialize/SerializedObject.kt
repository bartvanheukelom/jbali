package org.jbali.serialize

import org.jbali.util.stringToBeUnmarshalled
import java.io.Externalizable
import java.io.ObjectInput
import java.io.ObjectOutput
import java.io.Serializable

/**
 * [java.io.Serializable] reference to a Kotlin (static, singleton) `object`.
 *
 * - Serialized as the class name of the object.
 * - Unserialized by calling [Class.forName] on that name.
 *
 * Has no concept of versioning.
 */
class SerializedObject(
        var className: String
) : Externalizable {

    constructor() : this(stringToBeUnmarshalled)

    private fun readResolve() =
            Class.forName(className).kotlin.objectInstance

    companion object {
        const val serialVersionUID = 1L
    }

    override fun readExternal(ins: ObjectInput) {
        className = ins.readUTF()
    }

    override fun writeExternal(out: ObjectOutput) {
        out.writeUTF(className)
    }

}

abstract class SerializableObject : Serializable {

    protected fun writeReplace(): SerializedObject =
            SerializedObject(javaClass.name)

}
