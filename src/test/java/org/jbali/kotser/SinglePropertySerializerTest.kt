package org.jbali.kotser

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertSame

class SinglePropertySerializerTest {

    @Serializable(with = TestUserID.Serializer::class)
    data class TestUserID(val id: Long) {
        object Serializer : KSerializer<TestUserID> by singlePropertySerializer(
                prop = TestUserID::id,
                wrap = ::TestUserID
        )
    }


    @Test fun testSerializer() {

        val comp = TestUserID.serializer()
        val inlined = serializer<TestUserID>()
        assertSame(TestUserID.Serializer, comp)
        assertSame(TestUserID.Serializer, inlined)

//        @OptIn(ExperimentalSerializationApi::class)
//        assertEquals(TestUserID::class.qualifiedName, comp.descriptor.serialName)

    }

    @Test fun testJson() {
        TestUserID(123) shouldSerializeTo JsonPrimitive(123)
    }

}