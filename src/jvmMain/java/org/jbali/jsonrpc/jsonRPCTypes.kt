package org.jbali.jsonrpc

import kotlinx.serialization.Serializable

@Serializable
data class JsonRPCRequest<I : Any, P>(
    
    /**
     * When sending, don't change from the default value unless you know what you're doing.
     *
     * Note that the spec doesn't mention this property. Bitcoin Core includes it in its examples though, and it
     * seems like a useful extension.
     */
    val jsonrpc: String = "1.0",
    
    val method: String,
    
    val params: P?,
    
    /**
     * If `null`, this request is a [Notification][https://www.jsonrpc.org/specification_v1#a1.3Notification].
     */
    val id: I?,
    
)

@Serializable
data class JsonRPCResponse<I : Any, R, E : Any>(
    val result: R,
    val error: E?,
    val id: I,
)
