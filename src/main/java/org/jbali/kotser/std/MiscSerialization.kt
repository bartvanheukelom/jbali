package org.jbali.kotser.std

import com.google.common.net.InetAddresses
import kotlinx.serialization.Serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.serializersModuleOf
import org.jbali.kotser.StringBasedSerializer
import java.math.BigDecimal
import java.math.BigInteger
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

// --- InetAddress

@Suppress("UnstableApiUsage") // InetAddresses
@Serializer(forClass = InetAddress::class)
object InetAddressSerializer : StringBasedSerializer<InetAddress>(InetAddress::class) {
    override fun fromString(s: String): InetAddress = InetAddresses.forString(s)
    override fun toString(o: InetAddress): String = InetAddresses.toAddrString(o)
}

// TODO what happens when reading e.g. an ipv6 address as an Inet4Addres?
val inetAddressSerModule = serializersModuleOf(mapOf(
        InetAddress::class  to InetAddressSerializer,
        Inet4Address::class to InetAddressSerializer,
        Inet6Address::class to InetAddressSerializer
))


// --- BigDecimal

object BigDecimalSerializer : StringBasedSerializer<BigDecimal>(BigDecimal::class) {
    override fun fromString(s: String) = BigDecimal(s)
}

object BigIntegerSerializer : StringBasedSerializer<BigInteger>(BigInteger::class) {
    override fun fromString(s: String) = BigInteger(s)
}

val bigNumberSerModule =
        SerializersModule {
            contextual(BigDecimalSerializer)
            contextual(BigIntegerSerializer)
        }