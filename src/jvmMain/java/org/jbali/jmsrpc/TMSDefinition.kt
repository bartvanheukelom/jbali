package org.jbali.jmsrpc

import org.jbali.util.HasBetterKotlinAlternative
import kotlin.reflect.KClass

/**
 * Defines a [TextMessageService] by its interface and optional discriminator, to distinguish between multiple services with the same interface.
 */
data class TMSDefinition<T : Any>(
    val iface: KClass<T>,
    val discriminator: String? = null,
) {
    
    @JvmOverloads
    @HasBetterKotlinAlternative
    constructor(
        iface: Class<T>,
        discriminator: String? = null,
    ) : this(iface.kotlin, discriminator)
    
    val ifaceInfo = iface.asTMSInterface
    
    val uniqueName = iface.qualifiedName!! + (discriminator?.let { "[$it]" } ?: "")
    val path = iface.simpleName!! + (discriminator?.let { "/$it" } ?: "")
    val metricsName get() = ifaceInfo.metricsName
    
    override fun toString() = uniqueName
}

inline fun <reified I : Any> TMSDefinition(
    discriminator: String? = null,
) = TMSDefinition(
    iface = I::class,
    discriminator = discriminator,
)
