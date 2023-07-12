package org.jbali.random

import org.jbali.util.logger
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TestRandom : Random(0) {

    var count = 0
    var state = INIT_STATE
    var testedState = INIT_STATE
    var locked = true

    override fun next(bits: Int): Int {
        if (locked) throw AssertionError("TestRandom called while locked")
        val v = super.next(bits)
        count++
        state = "$count=$v"
        log.info("# random $state #")
        return v
    }

    fun assert(v: String): String {
        assertEquals(v, state, "TestRandom state")
        return v
    }

    fun assertInit() {
        assert(INIT_STATE)
    }

    fun assertChanged(v: String? = null) {
        if (v == null) {
            assertNotEquals(testedState, state, "TestRandom state")
        } else {
            assert(v)
        }
        testedState = state
    }

    fun assertUnchanged() {
        assert(testedState)
    }

    inline fun <T> allow(endState: String? = null, body: () -> T): T {
        val l = locked
        locked = false
        return try {
            body()
        } finally {
            locked = l
            if (endState != null) assert(endState)
        }
    }

    companion object {
        const val INIT_STATE = "init"
        val log = logger<TestRandom>()
    }
}