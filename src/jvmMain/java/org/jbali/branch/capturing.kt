@file:Suppress("UNCHECKED_CAST")

package org.jbali.branch

import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.suspendCoroutine

internal fun <T> runBranchingUsingCapture(block: suspend Branching.() -> T): Map<String, Result<T>> = buildMap {
    
    // guess we'll do it manually!
    val bklass = block.javaClass
    
//    val constrs = bklass.declaredConstructors
//        val constring = constrs.map { it.toString() }
//    val constr = constrs.singleOrNull {
//        it.parameterCount == tempContInitArgs.size + 1
//                && tempContInitArgs.indices.all { i ->
//            it.parameterTypes[i].isInstance(tempContInitArgs[i])
//        }
//                && it.parameterTypes.last() == Continuation::class.java
//    }
//        ?: error("$bklass must have a constructor with ${tempContInitArgs.size + 1} parameters, the last being Continuation")
    
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
    
    
    
    // run
    
    val completion = object : Continuation<Unit> {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            println("Coroutine completed with $result")
            result.getOrThrow()
        }
    }
    
//    val firstCont = constr.newInstance(*tempContInitArgs, null)
    val firstCont = block as Continuation<Any?>
    println("First continuation: $firstCont")

//            val secondCont = invokeSuspend.invoke(firstCont, Unit)
//            println("Second continuation: $secondCont")
    val branchCapture = CapturingBranching()
    
    val secondCont = invoke.invoke(firstCont, branchCapture, completion)
//        val secondCont = invokeSuspend.invoke(firstCont, Unit)
    println("First continuation, mutated: $firstCont")
    println("Second continuation: $secondCont")
    
    val stack = mutableListOf<String>()
    fun stackStr() = stack.joinToString("/")
    fun print(msg: String) {
        println("${stackStr()}>   $msg")
    }
    
    fun doBranch(base: Continuation<*>? = null) {
        
        val branchRes = branchCapture.takeRes()
        
        print("BRANCHING: ${branchRes.name} ** ${branchRes.options}")
        print("continuation: ${branchRes.continuation}")
        val sc = branchRes.continuation
        val del = base ?: sc
        
        // TODO cache
        val cc = ContinuationClass(del.javaClass)
//            println("label=${label.get(del)}")
        
        for (path in branchRes.options) {
//                    print("Resuming with $path")
            if (stack.size >= 1024) error("Too deep")
            stack.add(branchRes.name?.let { "$it=$path" }
                    ?: path.toString())
            try {
//                        println("Resuming")
                //                val clone = JavaSerializer.copy(del as Serializable) as Continuation<Any?>
                
                val clone = cc.clone(del)
                print("Cloned continuation: $clone")
                
                print("Calling invokeSuspend with value $path")
                val cloneCont = try {
                    cc.invokeSuspend.invoke(clone, path)
                } catch (ite: InvocationTargetException) {
                    print("Done with exception: ${ite.cause}")
                    put(stackStr(), Result.failure(ite.cause!!))
                    return
                }
                print("Clone $path continuation, mutated: $clone")
                print("Clone $path next continuation: $cloneCont")
                
                when {
                    cloneCont == COROUTINE_SUSPENDED -> {
                        if (branchCapture.didBranch()) {
                            print("Suspended, presumably for branching")
                            doBranch(clone) // TODO only works if we suspended in the same method
                        } else {
                            print("Suspended, not for branching, so...?")
                            TODO()
                        }
                    }
                    else -> {
                        print("Done I guess")
                        put(stackStr(), Result.success(cloneCont as T))
                    }
                }
                
            } finally {
                stack.removeAt(stack.lastIndex)
            }
            
        }
        
    }
    doBranch()
    
}

data class BranchPoint<B>(
    val name: String? = null,
    val options: List<B>,
    val continuation: Continuation<*>,
)

class CapturingBranching : Branching {
    
    private var res: BranchPoint<*>? = null
    
    
    fun didBranch() = res != null
    
    fun takeRes(): BranchPoint<*> {
        val r = res ?: error("Not branching?")
        res = null
        return r
    }
    
    override suspend fun <B> branch(
        name: String?,
        options: List<B>,
    ): B {
        return suspendCoroutine { cont ->
            val del = SafeCont.f_delegate.get(cont) as Continuation<*> // TODO check is SafeContinuation
            println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
            if (name != null) println("Branching off for $name!")
            else println("Branching off!")
            println("Options: $options")
            println("Continuation: $del")
            println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
            res = BranchPoint(name, options, del)
        }
    }
}
