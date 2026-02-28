@file:Suppress("UNCHECKED_CAST")

package org.jbali.branch

import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.suspendCoroutine

/**
 * Callback-based branching strategy: handles all branching inline within each branch point.
 *
 * When a [Branching.branch] call is encountered, this strategy immediately clones the
 * continuation for each option and resumes each clone within the same suspension callback.
 * This allows branching to work across multiple suspend function calls, since each branch
 * point is self-contained and doesn't rely on external coordination.
 *
 * Algorithm:
 * 1. Start executing the block
 * 2. When branch() is called with N options:
 *    - Clone the continuation N-1 times (reuse original for last option)
 *    - For each clone, immediately call invokeSuspend(option) within the callback
 *    - Each clone runs independently and may hit further branches
 *    - Store results in the shared map as they complete
 * 3. Return the map of all paths and their results
 *
 * @param block The suspend block to execute with branching support.
 * @return A map of path names to their results.
 */
internal fun <T> runBranchingUsingCallback(block: suspend Branching.() -> T): Map<String, Result<T>> {

    // Reflectively find the invoke method to start the coroutine manually
    val bklass = block.javaClass

    // Suspend lambdas compile to classes with an invoke(Receiver, Continuation) method
    val invoke = bklass.methods.singleOrNull {
        it.name == "invoke" && it.parameterCount == 2
                && it.parameterTypes[0] == Branching::class.java
                && it.parameterTypes[1] == Continuation::class.java
    }
        .let { it
            ?: error("Block must have a single invoke method with Branching and Continuation parameters")
        }

//        val bcc = ContinuationClass(bklass)

//        val label = fields.single { it.name == "label" }
    


    // Create a completion continuation that will be called when the coroutine finishes
    val completion = object : Continuation<Unit> {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            println("Coroutine completed with $result")
            result.getOrThrow()
        }
    }

    // The block itself is a continuation (suspend lambdas implement Continuation)
    val firstCont = block as Continuation<Any?>
    println("First continuation: $firstCont")

    // Create the branching context that will handle branch calls
    val branching = CallbackBranching<T>()

    // Start the coroutine by invoking the block with the branching context
    val secondCont = invoke.invoke(firstCont, branching, completion)
    println("First continuation, mutated: $firstCont")
    println("Second continuation: $secondCont")

    // Return the collected results from all branches
    return branching.map
    
}

/**
 * Branching implementation that handles all branching inline within each suspension point.
 *
 * This implementation maintains a path stack to track the current execution path and
 * stores results in a map as branches complete.
 */
class CallbackBranching<T> : Branching {

    /** Stack of branch decisions forming the current path (e.g., ["animal=Dog", "action=Bark"]) */
    val stack = mutableListOf<String>()

    /** Current path as a string (e.g., "animal=Dog/action=Bark") */
    fun stackStr() = stack.joinToString("/")

    /** Print with path prefix for debugging */
    fun print(msg: String) {
        println("${stackStr()}>   $msg")
    }

    /** Results map: path string -> result */
    val map = mutableMapOf<String, Result<T>>()

    /**
     * Branches execution into multiple paths by cloning and resuming the continuation
     * inline within this suspension callback.
     *
     * For N options, this creates N-1 clones and reuses the original for the last option.
     * Each clone is immediately resumed with its option value, and results are stored
     * as they complete.
     */
    override suspend fun <B> branch(
        name: String?,
        options: List<B>,
    ): B {
//        return suspendCoroutineUninterceptedOrReturn { c: Continuation<T> ->
//            val safe = SafeContinuation(c.intercepted())
//            block(safe)
//            safe.getOrThrow()
//        }
        return suspendCoroutine { cont ->

            // Unwrap SafeContinuation to get the actual continuation we can clone
            val del = SafeCont.f_delegate.get(cont) as Continuation<*> // TODO check is SafeContinuation

            println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
            if (name != null) println("Branching off for $name!")
            else println("Branching off!")
            println("Options: $options")
            println("Continuation: $del")
            println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

            // Reflect on the continuation class to enable cloning and invocation
            // TODO: cache ContinuationClass instances by class
            val cc = ContinuationClass(del.javaClass)

            // Process each option by cloning and resuming
            for ((i, path) in options.withIndex()) {
                if (stack.size >= 1024) error("Too deep")

                // Push this branch decision onto the stack
                stack.add(name?.let { "$it=$path" }
                    ?: path.toString())
                try {

                    // Optimization: reuse the original continuation for the last option
                    // to avoid an unnecessary clone
                    val clone = if (i == options.lastIndex) {
                        print("${if (options.size == 1) "Only" else "Last"} option, using original continuation")
                        del
                    } else {
                        cc.clone(del).also {
                            print("Cloned continuation: $it")
                        }
                    }

                    // Resume the clone with this option value
                    print("Calling invokeSuspend with value $path")
                    val cloneCont = try {
                        cc.invokeSuspend.invoke(clone, path)
                    } catch (ite: InvocationTargetException) {
                        // Exception thrown from within the coroutine
                        print("Done with exception: ${ite.cause}")
                        map[stackStr()] = Result.failure(ite.cause!!)
                        continue
                    }
                    print("Clone $path continuation, mutated: $clone")
                    print("Clone $path next continuation: $cloneCont")

                    // Check what happened after resuming
                    when {
                        cloneCont == COROUTINE_SUSPENDED -> {
                            // Clone suspended again (presumably another branch call)
                            // That branch call will handle itself via its own callback
                            println("Suspended, presumably for branching")
                        }
                        else -> {
                            // Clone completed with a result
                            println("Done I guess")
                            map[stackStr()] = Result.success(cloneCont as T)
                        }
                    }

                } finally {
                    // Pop this decision from the stack before processing next option
                    stack.removeAt(stack.lastIndex)
                }
            }

        }
    }
}
