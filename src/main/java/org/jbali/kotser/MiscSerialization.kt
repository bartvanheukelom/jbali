package org.jbali.kotser

import com.google.common.net.InetAddresses
import kotlinx.serialization.*
import kotlinx.serialization.modules.serializersModuleOf
import java.math.BigDecimal
import java.math.BigInteger
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

abstract class StringBasedSerializer<T> : KSerializer<T> {

    /**
     * If you get an error of the form:
     *    class WhateverSerializer overrides final method getDescriptor
     * You should not apply @Serializer(forClass=Whatever) to that class.
     * TODO removed final, what do?
     */
    override val descriptor = PrimitiveDescriptor(javaClass.name, PrimitiveKind.STRING)

    final override fun deserialize(decoder: Decoder): T =
            fromString(decoder.decodeString())

    final override fun serialize(encoder: Encoder, value: T) =
            encoder.encodeString(toString(value))

    abstract fun fromString(s: String): T
    open fun toString(o: T): String = o.toString()

}

// TODO make interface Transformer<T, F>, and a variant of this class which accepts an instance
abstract class TransformingSerializer<T, F>(
        val backend: KSerializer<F>
) : KSerializer<T> {

    /**
     * If you get an error of the form:
     *    class WhateverSerializer overrides final method getDescriptor
     * You should not apply @Serializer(forClass=Whatever::class) to that class.
     * To make a companion the default serializer, instead apply to main class:
     * @Serializable(with = Whatever.Companion::class)
     */
    final override val descriptor get() = backend.descriptor

    final override fun serialize(encoder: Encoder, value: T) {
        backend.serialize(encoder, transform(value))
    }

    final override fun deserialize(decoder: Decoder): T {
        return detransform(backend.deserialize(decoder))
    }

    abstract fun transform(obj: T): F
    abstract fun detransform(tf: F): T
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

object BigDecimalSerializer : StringBasedSerializer<BigDecimal>() {
    override fun fromString(s: String) = BigDecimal(s)
}

object BigIntegerSerializer : StringBasedSerializer<BigInteger>() {
    override fun fromString(s: String) = BigInteger(s)
}
