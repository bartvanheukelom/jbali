package org.jbali.jmsrpc

import kotlinx.serialization.Serializable
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import java.io.Serializable as JavaIoSerializable

data class JavaSerThingy(val x: Int) : JavaIoSerializable {
    companion object {
        const val serialVersionUID = 1L
    }
}
@Serializable
data class KoSeThingy(val x: Int)
@Serializable @JvmInline
value class KoSeInlineThingy(val y: String) {
    companion object {
        fun of(y: String) = KoSeInlineThingy(y)
    }
}

@JJS
interface TMSKotlinIfaceLayer1 {
    fun jjsEcho(x: JavaSerThingy): JavaSerThingy
    fun openJjsEcho(x: JavaSerThingy): JavaSerThingy?
}
@KoSe
interface TMSKotlinIfaceLayer2 : TMSKotlinIfaceLayer1 {
    fun koseEcho(x: KoSeThingy): KoSeThingy
    
    fun koseInlineEcho(y: KoSeInlineThingy): KoSeInlineThingy
    fun koseInlineParam(p: KoSeInlineThingy): Int
    fun koseInlineReturn(): KoSeInlineThingy
    fun koseInlineReturnBoxed(): TMSBox<KoSeInlineThingy>
    
    fun returningUnit()
    fun returningUnitOrNull(unit: Boolean): Unit?
    
    // custom serializers
    fun returningUuid(): UUID
    fun returningUuidOrNull(dewit: Boolean): UUID?
}

@KoSe
interface TMSKotlinIface : TMSKotlinIfaceLayer2 {
    
    fun withDefault(
        s: String = "hello"
    ): String
//    fun withNullable(
//        s: String?
//    ): String?
    
    @JJS // override will make this interface the "declaring class" - TODO is that what we want?
    override fun openJjsEcho(x: JavaSerThingy): JavaSerThingy
    
    fun returnChangedToSomething(): Int // was Unit
    fun returnNumberNarrowed(): Int // was Long
    fun returnTypeNarrowed(): String // was nullable
    
}
@KoSe
interface TMSKotlinIfaceOlder : TMSKotlinIfaceLayer2 {
    fun withDefault(
        // doesn't know about s yet
    ): String
//    fun withNullable(
//        // doesn't know about s yet
//    ): String?
    
    @JJS // see above
    override fun openJjsEcho(x: JavaSerThingy): JavaSerThingy
    
    fun returnChangedToSomething()
    fun returnNumberNarrowed(): Long
    fun returnTypeNarrowed(): String?
    
}

// called from TextMessageServiceTest, which is in Java and can't perform all of these
fun runTmsKotlinIfaceTest(clientK: TMSKotlinIfaceOlder) {
    
    // inline value classes are not supported, because of the name mangling
    // TODO fix
    assertFailsWith<TextMessageServiceClientException> {
        assertEquals("Hello", clientK.koseInlineEcho(KoSeInlineThingy("Hello")).y)
    }
    assertFailsWith<TextMessageServiceClientException> {
        clientK.koseInlineParam(KoSeInlineThingy("Hello"))
    }
    assertFailsWith<TextMessageServiceClientException> {
        assertEquals("X", clientK.koseInlineReturn().y)
    }
    
    assertEquals("X", clientK.koseInlineReturnBoxed().contents.y)
    
    
    // custom serializers
    assertEquals(UUID(12345678L, 87654321L), clientK.returningUuid())
    assertEquals(UUID(9999999L, 666666L), clientK.returningUuidOrNull(true))
    assertNull(clientK.returningUuidOrNull(false))
}

object TMSKotlinEndpoint : TMSKotlinIface {
    override fun withDefault(s: String): String = s
//    override fun withNullable(s: String?): String? = s
    override fun jjsEcho(x: JavaSerThingy) = x
    override fun koseEcho(x: KoSeThingy): KoSeThingy = x
    
    override fun koseInlineEcho(y: KoSeInlineThingy): KoSeInlineThingy = y
    override fun koseInlineParam(p: KoSeInlineThingy): Int = 555
    override fun koseInlineReturn(): KoSeInlineThingy = KoSeInlineThingy.of("X")
    override fun koseInlineReturnBoxed(): TMSBox<KoSeInlineThingy> = TMSBox(KoSeInlineThingy.of("X"))
    
    override fun openJjsEcho(x: JavaSerThingy): JavaSerThingy = x
    override fun returningUnit() {}
    override fun returningUnitOrNull(unit: Boolean) = if (unit) Unit else null
    override fun returnChangedToSomething(): Int = 43
    override fun returnNumberNarrowed(): Int = 55
    override fun returnTypeNarrowed(): String = "definitely a piece of text"
    
    override fun returningUuid(): UUID = UUID(12345678L, 87654321L)
    override fun returningUuidOrNull(dewit: Boolean): UUID? = if (dewit) UUID(9999999L, 666666L) else null
}
