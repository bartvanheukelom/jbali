package org.jbali.branch


fun <T> runBranching(block: suspend Branching.() -> T): Map<String, Result<T>> =
    runBranchingUsingCapture(block)


interface Branching {
    suspend fun <B> branch(name: String? = null, options: List<B>): B
    
    suspend fun <B> branch(options: List<B>): B = branch(null, options)
    
    // TODO causes too much overload ambiguity
//    suspend fun <B> branch(vararg options: B): B = branch(null, options.toList())
//    suspend fun <B> branch(name: String, vararg options: B): B = branch(name, options=options.toList())
}

