package org.jbali.kotser

import com.google.common.net.InetAddresses
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.modules.serializersModuleOf
import java.math.BigDecimal
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

abstract class StringBasedSerializer<T> : KSerializer<T> {

    override val descriptor = StringDescriptor

    final override fun deserialize(decoder: Decoder): T =
            fromString(decoder.decodeString())

    final override fun serialize(encoder: Encoder, obj: T) =
            encoder.encodeString(toString(obj))

    abstract fun fromString(s: String): T
    open fun toString(o: T): String = o.toString()

}

// --- InetAddress

@Suppress("UnstableApiUsage") // InetAddresses
@Serializer(forClass = InetAddress::class)
object InetAddressSerializer : StringBasedSerializer<InetAddress>() {
    override fun fromString(s: String): InetAddress = InetAddresses.forString(s)
    override fun toString(o: InetAddress): String = InetAddresses.toAddrString(o)
}

val inetAddressSerModule = serializersModuleOf(mapOf(
        InetAddress::class  to InetAddressSerializer,
        Inet4Address::class to InetAddressSerializer,
        Inet6Address::class to InetAddressSerializer
))


// --- BigDecimal

@Serializer(forClass = BigDecimal::class)
object BigDecimalSerializer : StringBasedSerializer<BigDecimal>() {
    override fun fromString(s: String) = BigDecimal(s)
}
