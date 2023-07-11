package org.jbali.random

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class TransparentRandomTest {
    @Test
    fun testEquivalent() {
        
        val seed = 548989929235L
        val official = Random(seed)
        val transparent = TransparentRandom.fromSeed(seed)
        repeat(16) {
            val on = official.nextInt()
//            println("on: ${on.toString().padStart(10)}")
            val tn = transparent.nextInt()
//            println("tn: ${tn.toString().padStart(10)}")
            println("${on.toString().padStart(10)}    ${tn.toString().padStart(10)}")
            assertEquals(on, tn)
        }
        
        val oh = official.nextHex(1024u)
        println("oh: $oh")
        val th = transparent.nextHex(1024u)
        println("th: $th")
        
        assertEquals(official.nextHex(1024u), transparent.nextHex(1024u))
    }
}

// copied/modified from jvmMain (TODO put in commonMain proper)
fun Random.nextHex(chars: UInt): String {
    require(chars % 2u == 0u)
    val bytes = chars / 2u
    return nextBytes(bytes.toInt()).toHex()
}
fun ByteArray.toHex() = joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
