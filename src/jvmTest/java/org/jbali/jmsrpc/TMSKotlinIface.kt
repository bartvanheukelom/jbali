package org.jbali.jmsrpc

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaIoSerializable

data class JavaSerThingy(val x: Int) : JavaIoSerializable {
    companion object {
        const val serialVersionUID = 1L
    }
}
@Serializable
data class KoSeThingy(val x: Int)

@JJS
interface TMSKotlinIfaceParent {
    fun jjsEcho(x: JavaSerThingy): JavaSerThingy
    fun openJjsEcho(x: JavaSerThingy): JavaSerThingy?
}

@KoSe
interface TMSKotlinIface : TMSKotlinIfaceParent {
    
    fun withDefault(
        s: String = "hello"
    ): String
//    fun withNullable(
//        s: String?
//    ): String?
    
    fun koseEcho(x: KoSeThingy): KoSeThingy
    @JJS // override will make this interface the "declaring class" - TODO is that what we want?
    override fun openJjsEcho(x: JavaSerThingy): JavaSerThingy
    
    fun returningUnit()
    fun returningUnitOrNull(unit: Boolean): Unit?
    fun returnChangedToSomething(): Int
    fun returnNumberNarrowed(): Int
    fun returnTypeNarrowed(): String
    
}
@KoSe
interface TMSKotlinIfaceOlder : TMSKotlinIfaceParent {
    fun withDefault(
        // doesn't know about s yet
    ): String
//    fun withNullable(
//        // doesn't know about s yet
//    ): String?
    
    fun koseEcho(x: KoSeThingy): KoSeThingy
    @JJS // see above
    override fun openJjsEcho(x: JavaSerThingy): JavaSerThingy
    
    fun returningUnit()
    fun returningUnitOrNull(unit: Boolean): Unit?
    fun returnChangedToSomething()
    fun returnNumberNarrowed(): Long
    fun returnTypeNarrowed(): String?
    
}

object TMSKotlinEndpoint : TMSKotlinIface {
    override fun withDefault(s: String): String = s
//    override fun withNullable(s: String?): String? = s
    override fun jjsEcho(x: JavaSerThingy) = x
    override fun koseEcho(x: KoSeThingy): KoSeThingy = x
    override fun openJjsEcho(x: JavaSerThingy): JavaSerThingy = x
    override fun returningUnit() {}
    override fun returningUnitOrNull(unit: Boolean) = if (unit) Unit else null
    override fun returnChangedToSomething(): Int = 43
    override fun returnNumberNarrowed(): Int = 55
    override fun returnTypeNarrowed(): String = "definitely a piece of text"
}
