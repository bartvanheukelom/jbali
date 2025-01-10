package org.jbali.net

import java.math.BigInteger
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.*


data class CIDRBlock<out I : InetAddress>(
    val address: I,
    val prefixLength: Int
) : Comparable<CIDRBlock<InetAddress>> {
    companion object {
        fun parse(line: String): CIDRBlock<InetAddress> {
            val (address, prefix) = line.split("/")
            return CIDRBlock(InetAddress.getByName(address), prefix.toInt())
        }
        fun <I : InetAddress> ofAddress(ip: I) = CIDRBlock(ip, if (ip is Inet4Address) 32 else 128)
    }
    
    init {
        require(
            (address is Inet4Address && prefixLength in 0..32) ||
                    (address is Inet6Address && prefixLength in 0..128)
        ) { "Invalid prefix length: $prefixLength for ${address::class.simpleName}" }
    }
    
    private val addressBytes = BigInteger(1, address.address)
    
    override fun toString(): String = "${address.hostAddress}/$prefixLength"
    
    operator fun contains(ip: InetAddress): Boolean {
        if (ip.javaClass != address.javaClass) return false
        
        val ipBytes = BigInteger(1, ip.address)
        
        val maskBits = if (address is Inet4Address) 32 else 128
        val mask = BigInteger.ONE.shiftLeft(maskBits - prefixLength).subtract(BigInteger.ONE).not()
        
        return (addressBytes and mask) == (ipBytes and mask)
    }
    
    @Suppress("UNCHECKED_CAST")
    fun as4() = if (address is Inet4Address) this as CIDRBlock<Inet4Address> else throw IllegalArgumentException("Not an IPv4 address block: $this")
    
    @Suppress("UNCHECKED_CAST")
    fun as6() = if (address is Inet6Address) this as CIDRBlock<Inet6Address> else throw IllegalArgumentException("Not an IPv6 address block: $this")
    
    private fun compareFamily(other: CIDRBlock<InetAddress>): Int? = when {
        address is Inet4Address && other.address is Inet6Address -> -1
        address is Inet6Address && other.address is Inet4Address -> 1
        else -> null
    }
    
    override fun compareTo(other: CIDRBlock<InetAddress>): Int =
        compareFamily(other)
            ?: addressBytes.compareTo(other.addressBytes).takeIf { it != 0 }
            ?: (prefixLength - other.prefixLength)
}


class CIDRBlocks<out I : InetAddress>(
    blocks: Iterable<CIDRBlock<I>>,
) {
    companion object {
        fun <I : InetAddress> of(blocks: Iterable<CIDRBlock<I>>) =
            CIDRBlocks(blocks.toCollection(TreeSet()))
    }
    
    private val blocks: TreeSet<CIDRBlock<InetAddress>> = blocks.toCollection(TreeSet())
    
    operator fun contains(ip: InetAddress): Boolean =
        blocks.floor(CIDRBlock.ofAddress(ip))?.let { ip in it } ?: false
    
    override fun toString(): String = blocks.joinToString(", ")
    
}
