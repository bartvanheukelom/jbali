package org.jbali.kotser

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ImplicitReflectionSerializer::class)
class SinglePropertySerializerTest {

    @Serializable(with = TestUserID.Serializer::class)
    data class TestUserID(val id: Long) {
        object Serializer : KSerializer<TestUserID> by singlePropertySerializer(
                prop = TestUserID::id,
                wrap = ::TestUserID
        )
    }

    @Test fun testSerializer() {

        assertEquals(TestUserID.Serializer, TestUserID.serializer())
        assertEquals(TestUserID.Serializer, serializer<TestUserID>())

        assertEquals(TestUserID::class.qualifiedName, TestUserID.serializer().descriptor.serialName)

    }

    @Test fun testJson() {
        TestUserID(123) shouldSerializeTo JsonPrimitive(123)
    }

}