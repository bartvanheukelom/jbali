package org.jbali.branch

/**
 * Strategy for handling continuation branching when exploring multiple execution paths.
 *
 * @property CAPTURING Capture-and-resume strategy: captures branch points as they occur,
 *           then clones and resumes continuations recursively from outside. Works well for
 *           simple cases but fails when branching spans multiple suspend function calls.
 *
 * @property CALLBACK Inline callback strategy: handles all branching immediately within
 *           each branch point by cloning and resuming continuations in the same suspension.
 *           Should work across multiple suspend function boundaries.
 */
enum class BranchingStrategy {
    CAPTURING,
    CALLBACK
}

/**
 * Executes a suspend block that can branch at decision points, exploring all possible
 * execution paths and returning a map of path names to their results.
 *
 * Each call to [Branching.branch] creates a fork in execution, with the path name built
 * from the branch names and chosen values (e.g., "category=Animals/animal=Dog").
 *
 * Example:
 * ```
 * runBranching {
 *     when (branch("animal", listOf(Dog, Cat))) {
 *         Dog -> "Woof"
 *         Cat -> when (branch("mood", listOf(Happy, Grumpy))) {
 *             Happy -> "Meow"
 *             Grumpy -> error("Hiss!")
 *         }
 *     }
 * }
 * // Returns: {
 * //   "animal=Dog" -> Success("Woof"),
 * //   "animal=Cat/mood=Happy" -> Success("Meow"),
 * //   "animal=Cat/mood=Grumpy" -> Failure(exception)
 * // }
 * ```
 *
 * @param strategy The branching strategy to use. See [BranchingStrategy] for details.
 * @param block The suspend block to execute with branching support.
 * @return A map of path names to their results (success or failure).
 */
fun <T> runBranching(
    strategy: BranchingStrategy = BranchingStrategy.CAPTURING,
    block: suspend Branching.() -> T
): Map<String, Result<T>> =
    when (strategy) {
        BranchingStrategy.CAPTURING -> runBranchingUsingCapture(block)
        BranchingStrategy.CALLBACK -> runBranchingUsingCallback(block)
    }

/**
 * Context interface providing the [branch] function for exploring multiple execution paths.
 *
 * This interface is provided as the receiver in [runBranching] blocks.
 */
interface Branching {
    /**
     * Suspends execution and branches into multiple paths, one for each option.
     *
     * The continuation at this point is cloned for each option, and each clone
     * is resumed with a different value, exploring all possible paths.
     *
     * @param name Optional name for this branch point, used in path strings.
     * @param options The list of values to explore. Must not be empty.
     * @return The value for this path (will differ across cloned continuations).
     */
    suspend fun <B> branch(name: String? = null, options: List<B>): B

    /**
     * Branches without a name. The path string will use the option value's toString().
     */
    suspend fun <B> branch(options: List<B>): B = branch(null, options)

    // TODO causes too much overload ambiguity
//    suspend fun <B> branch(vararg options: B): B = branch(null, options.toList())
//    suspend fun <B> branch(name: String, vararg options: B): B = branch(name, options=options.toList())
}

