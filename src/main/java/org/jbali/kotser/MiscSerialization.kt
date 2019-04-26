package org.jbali.kotser

import kotlinx.serialization.*
import kotlinx.serialization.modules.serializersModuleOf
import java.net.InetAddress

abstract class StringBasedSerializer<T> : KSerializer<T> {
    final override fun deserialize(input: Decoder): T =
            input.decodeString().let {
                fromString(it)
            }

    final override fun serialize(output: Encoder, obj: T) =
            output.encodeString(toString(obj))

    abstract fun fromString(s: String): T
    open fun toString(o: T): String = o.toString()

}

@Serializer(forClass = InetAddress::class)
object InetAddressSerializer : StringBasedSerializer<InetAddress>() {
//    // TODO if this class doesnt explicitly implement these, the compiler will generate an ov
//    override fun serialize(output: Encoder, obj: InetAddress) = xserialize(output, obj)
//    override fun deserialize(input: Decoder): InetAddress = deserialize(input)

    override fun fromString(s: String) = InetAddress.getByName(s)!!
    override fun toString(o: InetAddress) = o.hostAddress!!
}

object InetAddressSetSerializer : KSerializer<Set<InetAddress>> by InetAddressSerializer.set

val inetAddressSerModule = serializersModuleOf(InetAddress::class, InetAddressSerializer)
