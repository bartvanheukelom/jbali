@file:OptIn(ExperimentalSerializationApi::class)

package org.jbali.kotser

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.jbali.threads.withValue
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Serializable
@JvmInline
value class Name(val v: String)

enum class Workaround {
    DirectString,
    FromStringDescriptor,
    StringAsObject;
    
    companion object {
        val current = ThreadLocal<Workaround>()
    }
}

object FixedSerForName : KSerializer<Name> {
    
    override fun deserialize(decoder: Decoder): Name =
        Name(when (Workaround.current.get()!!) {
            Workaround.DirectString -> decoder.decodeString()
            Workaround.FromStringDescriptor -> decoder.decodeInline(descriptor).decodeString()
            Workaround.StringAsObject -> decoder.decodeSerializableValue(String.serializer())
        })
    
    override val descriptor: SerialDescriptor
        get() = String.serializer().descriptor
    
    override fun serialize(encoder: Encoder, value: Name) {
        when (Workaround.current.get()!!) {
            Workaround.DirectString -> encoder.encodeString(value.v)
            Workaround.FromStringDescriptor -> encoder.encodeInline(descriptor).encodeString(value.v)
            Workaround.StringAsObject -> encoder.encodeSerializableValue(String.serializer(), value.v)
        }
    }
    
}



@Serializable(with=NameFixed.Companion::class)
@JvmInline
value class NameFixed(private val v: String) {
    
    /*
        - enabling this annotation and commenting out `override val descriptor` results in:
            class org.jbali.kotser.NameFixed$Companion cannot be cast to class kotlinx.serialization.internal.GeneratedSerializer
        - enabling it while keeping `override val descriptor` has no apparent effect
     */
//    @Serializer(forClass = NameFixed::class)
    companion object : KSerializer<NameFixed> {
        
        override fun deserialize(decoder: Decoder): NameFixed =
            NameFixed(when (Workaround.current.get()!!) {
                Workaround.DirectString -> decoder.decodeString()
                Workaround.FromStringDescriptor -> decoder.decodeInline(descriptor).decodeString()
                Workaround.StringAsObject -> decoder.decodeSerializableValue(String.serializer())
            })
    
        override val descriptor: SerialDescriptor
            get() = String.serializer().descriptor
    
        override fun serialize(encoder: Encoder, value: NameFixed) {
            when (Workaround.current.get()!!) {
                Workaround.DirectString -> encoder.encodeString(value.v)
                Workaround.FromStringDescriptor -> encoder.encodeInline(descriptor).encodeString(value.v)
                Workaround.StringAsObject -> encoder.encodeSerializableValue(String.serializer(), value.v)
            }
        }
    
    }
}

@Serializable
data class Person(val name: Name)

@RunWith(Parameterized::class)
class TaggedEncoderBugTest(
    private val w: Workaround?,
) {
    
    companion object {
        @Parameterized.Parameters(name="w={0}")
        @JvmStatic fun params() = listOf(null) + Workaround.values()
    }
    
    @Test fun testEncodeInlineClassToJsonElement() {
    
        val json = DefaultJson.plainOmitDefaults
        
        if (w == null) {
            
            // directly to string works
            assertEquals(
                """"henk"""",
                json.encodeToString(Name("henk")),
            )
            // but to element fails, with the exact same serializer and value
            assertFailsWith<SerializationException> {
                json.encodeToJsonElement(Name("henk"))
            }.let { e ->
                assertEquals("No tag in stack for requested element", e.message)
            }
    
            // and what if we wrap it?
            val geert = Person(Name("geert"))
            // directly to string works, as expected
            assertEquals(
                """{"name":"geert"}""",
                json.encodeToString(geert),
            )
            // but somewhat surprisingly, to element also works
            assertEquals(
                buildJsonObject { put("name", jsonString("geert")) },
                json.encodeToJsonElement(geert)
            )
    
        } else {
            Workaround.current.withValue(w) {
                
                // check that the workaround produces the correct string
                assertEquals(
                    """"ingrid"""",
                    json.encodeToString(NameFixed("ingrid")),
                )
                // all the workarounds allow producing a valid element
                assertEquals(
                    jsonString("ingrid"),
                    json.encodeToJsonElement(NameFixed("ingrid"))
                )
    
                // and what if we use a custom serializer that is not the default for the type?
                assertEquals(
                    """"ingrid"""",
                    json.encodeToString(FixedSerForName, Name("ingrid")),
                )
                assertEquals(
                    jsonString("ingrid"),
                    json.encodeToJsonElement(FixedSerForName, Name("ingrid"))
                )
            }
        }
    
    }
    
}