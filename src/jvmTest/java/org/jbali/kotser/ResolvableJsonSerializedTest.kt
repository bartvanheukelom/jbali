package org.jbali.kotser

import kotlinx.serialization.Serializable
import org.jbali.json2.JSONString
import org.jbali.serialize.JavaSerializer
import org.jbali.util.stringToBeUnmarshalled
import java.io.Serial
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
internal data class Flubber(
    val flying: Boolean = false,
) : java.io.Serializable {
    
    companion object { @Serial const val serialVersionUid = 1L }
    class JsonSerialized(json: JSONString) : ResolvableJsonSerializedBase<Flubber>(json) {
        internal constructor() : this(JSONString(stringToBeUnmarshalled))
        companion object : CompanionBase<Flubber>(Flubber::class) {
            @Serial const val serialVersionUid = 1L
        }
        override val jsonSerializer get() = classJsonSerializer
    }
    @Serial fun writeReplace() = JsonSerialized.writeReplace(this)
    
}

class ResolvableJsonSerializedTest {
    
    @Test fun test() {
        assertEquals(Flubber(flying = true), Flubber.JsonSerialized(JSONString("""
            {"flying": true}
        """)).readResolve())
        assertEquals(Flubber(flying = false), Flubber.JsonSerialized(JSONString("{}")).readResolve())
    
        val flub = Flubber(flying = true)
        assertEquals(flub, JavaSerializer.copy(flub))
    }
    
}