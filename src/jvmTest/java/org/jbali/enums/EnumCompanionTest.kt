package org.jbali.enums

import org.junit.Assert.assertEquals
import kotlin.test.Test


enum class SpiceGirl(
    val shortName: String,
) {
    Scary("Mel B"),
    Sporty("Mel C"),
    Ginger("Geri"),
    Baby("Emma"),
    Posh("Victoria");
    
    init { println("init $this") }
    
    companion object : EnumCompanion<SpiceGirl>(SpiceGirl::class) {
        init { println("init $this") }
        val mels = setOf(Scary, Sporty)
        init { println("/init $this; mels=$mels") }
    }
    
    init { println("/init $this") }
    
    // output:
    // init Scary
    // /init Scary
    // init Sporty
    // /init Sporty
    // ...
    // init EnumCompanion(org.jbali.enums.SpiceGirl)
    // /init EnumCompanion(org.jbali.enums.SpiceGirl); mels=[Scary, Sporty]
}

class EnumCompanionTest {
    
    @Test fun test() {
        val expectAll = setOf(SpiceGirl.Scary, SpiceGirl.Sporty, SpiceGirl.Ginger, SpiceGirl.Baby, SpiceGirl.Posh)
        val expectMels = setOf(SpiceGirl.Scary, SpiceGirl.Sporty)
        
        assertEquals(expectAll, SpiceGirl.all)
        assertEquals(expectMels, SpiceGirl.mels)
    }
    
}
