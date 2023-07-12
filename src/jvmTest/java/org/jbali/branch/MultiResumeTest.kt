@file:Suppress("USELESS_IS_CHECK", "SENSELESS_COMPARISON", "UNNECESSARY_SAFE_CALL")

package org.jbali.branch

import org.jbali.random.nextHex
import org.jbali.test.assertEquals
import org.junit.Test
import kotlin.concurrent.thread
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue


enum class Category {
    Animals, Plants
}
enum class Animal {
    Dog, Cat
}
enum class Plant {
    Tree, Bush
}

class MultiResumeTest {
    
    // P = Capturing
    // B = Callback
    
    // P: Pass
    // B: ?
    @Test
    fun testLambda() {
        runBranching {
            
            val scope = this
            println("Scope: $scope")
            checkScope(scope)
            
            val veryObnoxiouslyVisibleContinuationVal = Random.nextHex(4u)
            var msg = "$veryObnoxiouslyVisibleContinuationVal --- "
            
            // BRANCH //
            val category = branch("category", Category.values().toList())
            println("RESUME with category=$category")
            check(category is Category) {
                "Expected Category, got ${category?.javaClass}"
            }
            
            msg += "$category: "
            when (category) {
                Category.Animals -> {
                    
                    // BRANCH //
                    val animal = branch("animal", Animal.values().toList())
                    println("RESUME with animal=$animal")
                    check(animal is Animal) {
                        "Expected Animal, got ${animal?.javaClass}"
                    }
                    
                    msg += animal
                }
                Category.Plants -> {
                    
                    // BRANCH //
                    val plant = branch("plant", Plant.values().toList())
                    println("RESUME with plant=$plant")
                    check(plant is Plant) {
                        "Expected Plant, got ${plant?.javaClass}"
                    }
                    
                    msg += plant
                }
            }
            
            msg
            
        }.forEach { (key, value) ->
            println("$key: $value")
        }
    }
    
    // P: Pass
    // B: ?
    @Test fun testResumeForward() {
        var resumed = ""
        runBranching {
            val a = branch("A", listOf("a")) // NOTE: not even branching
            assertEquals("", resumed); resumed = "A"
            assertEquals("a", a)
            
            val b = branch("B", listOf("b"))
            assertEquals("A", resumed); resumed = "B"
            assertEquals("b", b)
            
            // NOTE: literally even branching
            val zz = branch("ZZ", listOf("z", "Z"))
            assertTrue(resumed == "B" || resumed == "ZZ"); resumed = "ZZ"
            assertTrue(zz == "z" || zz == "Z")
        }
    }
    
    // P: Pass
    // B: ?
    @Test fun testExample() {
        runBranching {
            when (branch("animal", Animal.values().toList())) {
                Animal.Dog ->
                    "Woof"
                Animal.Cat ->
                    when (branch("goodMood", listOf(true, false))) {
                        true -> "Meow"
                        false -> error("Leave me alone, I'm sleeping in a box")
                    }
            }
        }
            .forEach { (k, v) -> println("$k: $v") }
    }
    
    // P: FAIL
    // B: ?
    @Test
    fun testMemberFun() {
        runBranching {
            voice()
        }
            .also {
                assertEquals(mapOf(
                    "Dog" to Result.success("Woof"),
                    "Cat>false" to Result.success("Meow"),
                    "Cat>true" to it["Cat>true"],
                ), it)
                assertEquals(
                    "Leave me alone, I'm sleeping in a box",
                    it["Cat>true"]?.exceptionOrNull()?.message
                )
            }
            .forEach { (k, v) -> println("$k: $v") }
    }
    
    private suspend fun sleep(ms: Long) {
        suspendCoroutine<Unit>{ cont ->
            thread {
                Thread.sleep(ms)
                cont.resumeWith(Result.success(Unit))
            }
        }
    }
    
    private suspend fun Branching.voice(): String {
        val animal = branch(Animal.values().toList())
        return when (animal) {
            Animal.Dog -> "Woof"
            Animal.Cat -> {
                
//                sleep(1000)
//                sleepy() // different continuation class
                
                when (boolz()) {
                    true -> "Meow"
                    false -> error("Leave me alone, I'm sleeping in a box")
                }
            }
        }
    }
    
    private suspend fun sleepy() {
        sleep(1000)
    }
    
    private suspend fun Branching.boolz(): Boolean {
        // printlns are to force some function state
        println("boolz...")
        val b = branch(listOf(1, 0, 42))
        println("boolz: RESUME with $b")
        // Int -> Bool, just to make sure we're not skipping this step (as in, suspend at branch here, but resume at our caller directly)
        return b == 1
    }
    
    private fun checkScope(scope: Branching) {
        if (scope == null) {
            error("Scope is null")
        }
    }
    
}
