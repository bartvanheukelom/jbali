package org.jbali.kotser.std

import com.google.common.net.HostAndPort
import com.google.common.net.InetAddresses
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.jbali.kotser.StringBasedSerializer
import java.math.BigDecimal
import java.math.BigInteger
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.*

object UUIDSerializer : StringBasedSerializer<UUID>(UUID::class) {
    override fun fromString(s: String): UUID = UUID.fromString(s)
}

@Suppress("UnstableApiUsage")
object HostAndPortSerializer : StringBasedSerializer<HostAndPort>(HostAndPort::class) {
    override fun fromString(s: String): HostAndPort = HostAndPort.fromString(s)
}


// --- InetAddress

@Suppress("UnstableApiUsage") // InetAddresses
object InetAddressSerializer : StringBasedSerializer<InetAddress>(InetAddress::class) {
    override fun fromString(s: String): InetAddress = InetAddresses.forString(s)
    override fun toString(o: InetAddress): String = InetAddresses.toAddrString(o)
}
@Suppress("UnstableApiUsage") // InetAddresses
object Inet4AddressSerializer : StringBasedSerializer<Inet4Address>(Inet4Address::class) {
    override fun fromString(s: String): Inet4Address = InetAddresses.forString(s) as Inet4Address
    override fun toString(o: Inet4Address): String = InetAddresses.toAddrString(o)
}
@Suppress("UnstableApiUsage") // InetAddresses
object Inet6AddressSerializer : StringBasedSerializer<Inet6Address>(Inet6Address::class) {
    override fun fromString(s: String): Inet6Address = InetAddresses.forString(s) as Inet6Address
    override fun toString(o: Inet6Address): String = InetAddresses.toAddrString(o)
}

val inetAddressSerModule = SerializersModule {
    contextual(InetAddressSerializer)
    contextual(Inet4AddressSerializer)
    contextual(Inet6AddressSerializer)
}


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