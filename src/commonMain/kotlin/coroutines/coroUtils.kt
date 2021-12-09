package org.jbali.coroutines

import org.jbali.util.FakeExpression


/**
 * IntelliJ IDEA can use this to detect when blocking code is inappropriately
 * called in a suspending context.
 *
 * For this to work, add the name of this annotation to the settings of the
 * "Inappropriate thread-blocking method call" inspection.
 */
annotation class Blocking

/**
 * See [Blocking].
 */
annotation class NonBlocking


/**
 * Suspending facade for blocking interface [I].
 * Allows calling methods of that interface from suspending code.
 */
interface Suspending<out I : Any> {

//    val blocking: I
    
    /**
     * Within an appropriate context, run [call] which must call exactly one method of its receiver.
     */
    suspend operator fun <R> invoke(call: I.() -> R): R
    
}

private fun suspendingCompileCheck() {
    val col: Suspending<Collection<Int>> = FakeExpression.of<Suspending<List<Int>>>()
}