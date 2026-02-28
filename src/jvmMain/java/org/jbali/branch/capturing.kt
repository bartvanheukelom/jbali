@file:Suppress("UNCHECKED_CAST")

package org.jbali.branch

import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.suspendCoroutine

/**
 * Capture-based branching strategy: captures branch points externally and resumes recursively.
 *
 * When a [Branching.branch] call is encountered, this strategy captures the branch point
 * (options and continuation) and returns control to the outer coordinator. The coordinator
 * then clones the continuation for each option and resumes each clone. If a clone suspends
 * again, the coordinator recursively handles the next branch.
 *
 * Algorithm:
 * 1. Start executing the block
 * 2. When branch() is called:
 *    - Capture the continuation and options in a BranchPoint
 *    - Return control to coordinator (suspension)
 * 3. Coordinator receives BranchPoint:
 *    - Clone continuation N times
 *    - Resume each clone with an option value
 *    - If clone suspends again, recursively handle next BranchPoint
 *    - If clone completes, store result
 * 4. Return map of all paths and results
 *
 * Limitation: Only works when all branch points occur in the same continuation class.
 * Fails when branching spans multiple suspend function calls, because the recursive
 * call assumes it's resuming the same continuation that was captured.
 *
 * @param block The suspend block to execute with branching support.
 * @return A map of path names to their results.
 */
internal fun <T> runBranchingUsingCapture(block: suspend Branching.() -> T): Map<String, Result<T>> = buildMap {

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

    // Create the capturing branching context
    val branchCapture = CapturingBranching()

    // Start the coroutine by invoking the block with the branching context
    val secondCont = invoke.invoke(firstCont, branchCapture, completion)
    println("First continuation, mutated: $firstCont")
    println("Second continuation: $secondCont")

    /** Stack of branch decisions forming the current path */
    val stack = mutableListOf<String>()

    /** Current path as a string */
    fun stackStr() = stack.joinToString("/")

    /** Print with path prefix for debugging */
    fun print(msg: String) {
        println("${stackStr()}>   $msg")
    }

    /**
     * Recursively processes a captured branch point by cloning and resuming continuations.
     *
     * @param base Optional continuation to use instead of the captured one. Used when
     *             recursively handling nested branches - we need to resume the mutated clone,
     *             not the original captured continuation.
     */
    fun doBranch(base: Continuation<*>? = null) {

        // Take the captured branch point from the branching context
        val branchRes = branchCapture.takeRes()

        print("BRANCHING: ${branchRes.name} ** ${branchRes.options}")
        print("continuation: ${branchRes.continuation}")
        val sc = branchRes.continuation
        val del = base ?: sc

        // Reflect on the continuation class to enable cloning and invocation
        // TODO: cache ContinuationClass instances by class
        val cc = ContinuationClass(del.javaClass)

        // Process each option by cloning and resuming
        for (path in branchRes.options) {
            if (stack.size >= 1024) error("Too deep")

            // Push this branch decision onto the stack
            stack.add(branchRes.name?.let { "$it=$path" }
                    ?: path.toString())
            try {

                // Clone the continuation to explore this path independently
                val clone = cc.clone(del)
                print("Cloned continuation: $clone")

                // Resume the clone with this option value
                print("Calling invokeSuspend with value $path")
                val cloneCont = try {
                    cc.invokeSuspend.invoke(clone, path)
                } catch (ite: InvocationTargetException) {
                    // Exception thrown from within the coroutine
                    print("Done with exception: ${ite.cause}")
                    put(stackStr(), Result.failure(ite.cause!!))
                    return
                }
                print("Clone $path continuation, mutated: $clone")
                print("Clone $path next continuation: $cloneCont")

                // Check what happened after resuming
                when {
                    cloneCont == COROUTINE_SUSPENDED -> {
                        if (branchCapture.didBranch()) {
                            // Suspended and captured another branch point
                            print("Suspended, presumably for branching")
                            // Recursively handle the next branch point
                            // LIMITATION: This only works if we suspended in the same method/continuation class!
                            // If the suspension happened in a different suspend function, we're resuming
                            // the wrong continuation here.
                            doBranch(clone) // TODO only works if we suspended in the same method
                        } else {
                            // Suspended but not for branching (e.g., real async operation)
                            print("Suspended, not for branching, so...?")
                            TODO()
                        }
                    }
                    else -> {
                        // Clone completed with a result
                        print("Done I guess")
                        put(stackStr(), Result.success(cloneCont as T))
                    }
                }

            } finally {
                // Pop this decision from the stack before processing next option
                stack.removeAt(stack.lastIndex)
            }

        }

    }

    // Start the branching process
    doBranch()

}

/**
 * Represents a captured branch point with its options and the continuation to resume.
 *
 * @param name Optional name for this branch point, used in path strings.
 * @param options The list of option values to explore.
 * @param continuation The continuation captured at the branch point, which will be
 *                     cloned and resumed for each option.
 */
data class BranchPoint<B>(
    val name: String? = null,
    val options: List<B>,
    val continuation: Continuation<*>,
)

/**
 * Branching implementation that captures branch points and returns control to the coordinator.
 *
 * When [branch] is called, it captures the continuation and options in a [BranchPoint]
 * and suspends, allowing the outer coordinator to handle the actual cloning and resuming.
 */
class CapturingBranching : Branching {

    /** The most recently captured branch point, or null if none captured */
    private var res: BranchPoint<*>? = null

    /** Returns true if a branch point has been captured since the last takeRes() */
    fun didBranch() = res != null

    /**
     * Takes and clears the captured branch point.
     * @throws IllegalStateException if no branch point is currently captured.
     */
    fun takeRes(): BranchPoint<*> {
        val r = res ?: error("Not branching?")
        res = null
        return r
    }

    /**
     * Captures a branch point and suspends, returning control to the coordinator.
     *
     * The continuation is captured and stored along with the options. The coordinator
     * will clone this continuation and resume each clone with a different option.
     */
    override suspend fun <B> branch(
        name: String?,
        options: List<B>,
    ): B {
        return suspendCoroutine { cont ->
            // Unwrap SafeContinuation to get the actual continuation we can clone
            val del = SafeCont.f_delegate.get(cont) as Continuation<*> // TODO check is SafeContinuation
            println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
            if (name != null) println("Branching off for $name!")
            else println("Branching off!")
            println("Options: $options")
            println("Continuation: $del")
            println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
            // Store the branch point for the coordinator to retrieve
            res = BranchPoint(name, options, del)
        }
    }
}
