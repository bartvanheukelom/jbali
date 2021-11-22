package org.jbali.jmsrpc

interface TMSKotlinIface {
    
    fun withDefault(
        s: String = "hello"
    ): String
    fun withNullable(
        s: String?
    ): String?
    
}
interface TMSKotlinIfaceOlder {
    fun withDefault(
        // doesn't know about s yet
    ): String
    fun withNullable(
        // doesn't know about s yet
    ): String?
}

object TMSKotlinEndpoint : TMSKotlinIface {
    override fun withDefault(s: String): String = s
    override fun withNullable(s: String?): String? = s
}
