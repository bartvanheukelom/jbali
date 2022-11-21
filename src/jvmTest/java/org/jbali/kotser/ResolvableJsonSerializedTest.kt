package org.jbali.kotser

import kotlinx.serialization.Serializable
import org.jbali.json2.JSONString
import org.jbali.serialize.JavaSerializer
import org.jetbrains.annotations.TestOnly
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Serial
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolvableJsonSerializedTest {
    
    val log = LoggerFactory.getLogger(ResolvableJsonSerializedTest::class.java)
    
    @Test fun test() {
        
        log.info("test: resolve flying=true")
        assertEquals(Flubber(flying = true ), Flubber.JsonSerialized("""{"flying": true}""").readResolve())
        log.info("test: resolve flying=undefined")
        assertEquals(Flubber(flying = false), Flubber.JsonSerialized("""{              }""").readResolve())
    
        log.info("test: copy")
        val flub = Flubber(flying = true)
        assertEquals(flub, JavaSerializer.copy(flub))
    }
    
}

@Serializable
internal data class Flubber(
    val flying: Boolean = false,
) : java.io.Serializable {
    
    companion object {
        @Serial const val serialVersionUid = 1L
        val log: Logger = LoggerFactory.getLogger(Flubber::class.java)
    }
    
    init {
        log.info("constructing $this")
    }
    
    class JsonSerialized constructor() : ResolvableJsonSerializedBase<Flubber>() {
        
        init {
            log.info("${javaClass.name} constructing with json=$json")
        }
        
        @TestOnly
        constructor(jsonStr: String) : this() {
            json = JSONString(jsonStr)
        }
        
        companion object : CompanionBase<Flubber>(Flubber::class) {
            @Serial const val serialVersionUid = 1L
        }
        override val jsonSerializer get() = classJsonSerializer
    
        @set:TestOnly
        override var json: JSONString
            get() = super.json
            set(v) {
                log.info("$this.json = $v")
                super.json = v
            }
    
        @TestOnly
//        @Serial override fun readResolve(): Flubber { // TODO this caused the test to fail, why?
        override fun trueReadResolve(): Flubber {
            log.info("$this.readResolve() = ...")
            val res = super.trueReadResolve()
            log.info("$this.trueReadResolve() = $res")
            return res
        }
    }
    @Serial fun writeReplace(): Any {
        log.info("$this.writeReplace() = ...")
        val replace = JsonSerialized.writeReplace(this)
        log.info("$this.writeReplace() = $replace")
        return replace
    }
    
}
