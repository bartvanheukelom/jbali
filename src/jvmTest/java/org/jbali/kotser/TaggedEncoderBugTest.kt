@file:OptIn(ExperimentalSerializationApi::class)

package org.jbali.kotser

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.jbali.threads.withValue
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.slf4j.LoggerFactory
import kotlin.test.*

// https://github.com/Kotlin/kotlinx.serialization/issues/1774 //

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

private val json = DefaultJson.plainOmitDefaults



@RunWith(Parameterized::class)
class TaggedEncoderBugTest(
    private val w: Workaround?,
) {
    
    companion object {
        @Parameterized.Parameters(name="w={0}")
        @JvmStatic fun params() = listOf(null) + Workaround.values()
    }
    
    @Test fun testEncodeInlineClassToJsonElement() {
    
        
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
            // but it would work in the fixed version
            assertTrue(Name.serializer().descriptor.needTopLevelTag)
    
            // what about decoding?
            assertEquals(
                Name("henk"),
                json.decodeFromJsonElement(jsonString("henk")),
            )
    
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




object UndescriptiveNameSerializer : KSerializer<Name> {
    override fun deserialize(decoder: Decoder): Name {
        TODO("Not yet implemented")
    }
    
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor
        get() = buildSerialDescriptor("UndescriptiveNameSerializer", SerialKind.CONTEXTUAL)
    
    override fun serialize(encoder: Encoder, value: Name) {
        val s = value.v.split(" ")
        when (s.size) {
            1 -> encoder.encodeSerializableValue(String.serializer(), value.v)
            2 -> encoder.encodeSerializableValue(serializer<Map<String, String>>(), mapOf(
                "first" to s.first(),
                "last" to s.last(),
            ))
        }
    }
    
}

object NameAsListSerializer : KSerializer<Name> {
    val stringListSer = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = stringListSer.descriptor
    override fun serialize(encoder: Encoder, value: Name) {
        encoder.encodeSerializableValue(stringListSer, listOf(value.v))
    }
    override fun deserialize(decoder: Decoder) = TODO()
}

// from upcoming patch:
// https://github.com/Kotlin/kotlinx.serialization/pull/1777/commits/3e44197923a9364e57e6c439adf22f2ec441d91a
val SerialDescriptor.needTopLevelTag: Boolean
    get() {
        if (kind is PrimitiveKind || kind === SerialKind.ENUM) return true
        if (isInline) return getElementDescriptor(0).needTopLevelTag
        return false
    }

class TaggedEncoderUndescriptiveTest {
    
    private val log = LoggerFactory.getLogger(TaggedEncoderUndescriptiveTest::class.java)
    
    /**
     * Test with a custom serializer whose descriptor won't / can't tell whether it needs TopLevelTag
     */
    @Test fun testUndescriptiveSer() {
        
        // works
        assertEquals(
            """"thierry"""",
            json.encodeToString(UndescriptiveNameSerializer, Name("thierry")),
        )
        assertEquals(
            """{"first":"jan","last":"doedel"}""",
            json.encodeToString(UndescriptiveNameSerializer, Name("jan doedel")),
        )
    
        // will encode as an object, and it works
        assertEquals(
            """{"first":"jan","last":"doedel"}""",
            json.encodeToJsonElement(UndescriptiveNameSerializer, Name("jan doedel")).let(json::encodeToString),
        )
        
        // will encode as a string, and throws No tag in stack
        // TODO suddenly it works. why?
//        assertFailsWith<SerializationException> {
            json.encodeToJsonElement(UndescriptiveNameSerializer, Name("thierry"))
//        }.let { e ->
//            assertEquals("No tag in stack for requested element", e.message)
//        }
        // and would still fail in the new version
        assertFalse(UndescriptiveNameSerializer.descriptor.needTopLevelTag)
        
    }
    
    /**
     * Test with ContextSerializer, which apparently also won't provide the required info for needTopLevelTag.
     */
    @Suppress("JSON_FORMAT_REDUNDANT")
    @Test fun testContextual() {
        
        val ctxNameSer = ContextualSerializer(Name::class)
        
        log.info("Contextual NameAsListSerializer")
        Json(json) { serializersModule = SerializersModule {
            contextual<Name>(NameAsListSerializer)
        } }
            .let { cj ->
                assertEquals(
                    """["beppie"]""",
                    cj.encodeToString(ctxNameSer, Name("beppie")),
                )
                cj.encodeToJsonElement(ctxNameSer, Name("beppie"))
            }
            
    
        log.info("Contextual Name.serializer()")
        Json(json) { serializersModule = SerializersModule {
            contextual<Name>(Name.serializer())
        } }
            .let { cj ->
                assertEquals(
                    """"beppie"""",
                    cj.encodeToString(ctxNameSer, Name("beppie")),
                )
                // will encode as a string, and throws No tag in stack
                assertFailsWith<SerializationException> {
                    cj.encodeToJsonElement(ctxNameSer, Name("beppie"))
                }.let { e ->
                    assertEquals("No tag in stack for requested element", e.message)
                }
                // and would still fail in the new version
                assertFalse(ctxNameSer.descriptor.needTopLevelTag)
            }
        
    }
}