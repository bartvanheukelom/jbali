@file:Suppress("UNCHECKED_CAST")

package org.jbali.branch

import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.suspendCoroutine

internal fun <T> runBranchingUsingCallback(block: suspend Branching.() -> T): Map<String, Result<T>> {
    
    // guess we'll do it manually!
    val bklass = block.javaClass
    
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
    val branching = CallbackBranching<T>()
    
    val secondCont = invoke.invoke(firstCont, branching, completion)
//        val secondCont = invokeSuspend.invoke(firstCont, Unit)
    println("First continuation, mutated: $firstCont")
    println("Second continuation: $secondCont")
    
    return branching.map
    
}


class CallbackBranching<T> : Branching {
    
    val stack = mutableListOf<String>()
    fun stackStr() = stack.joinToString("/")
    fun print(msg: String) {
        println("${stackStr()}>   $msg")
    }
    
    val map = mutableMapOf<String, Result<T>>()
    
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
            
            val del = SafeCont.f_delegate.get(cont) as Continuation<*> // TODO check is SafeContinuation
            
            println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
            if (name != null) println("Branching off for $name!")
            else println("Branching off!")
            println("Options: $options")
            println("Continuation: $del")
            println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
            
            // TODO cache
            val cc = ContinuationClass(del.javaClass)
            
            for ((i, path) in options.withIndex()) {
                if (stack.size >= 1024) error("Too deep")
                stack.add(name?.let { "$it=$path" }
                    ?: path.toString())
                try {
                    
                    val clone = if (i == options.lastIndex) {
                        print("${if (options.size == 1) "Only" else "Last"} option, using original continuation")
                        del
                    } else {
                        cc.clone(del).also {
                            print("Cloned continuation: $it")
                        }
                    }
                    
                    
                    print("Calling invokeSuspend with value $path")
                    val cloneCont = try {
                        cc.invokeSuspend.invoke(clone, path)
                    } catch (ite: InvocationTargetException) {
                        print("Done with exception: ${ite.cause}")
                        map[stackStr()] = Result.failure(ite.cause!!)
                        continue
                    }
                    print("Clone $path continuation, mutated: $clone")
                    print("Clone $path next continuation: $cloneCont")
                    
                    when {
                        cloneCont == COROUTINE_SUSPENDED -> {
                            println("Suspended, presumably for branching")
                        }
                        else -> {
                            println("Done I guess")
                            map[stackStr()] = Result.success(cloneCont as T)
                        }
                    }
                    
                } finally {
                    stack.removeAt(stack.lastIndex)
                }
            }
            
        }
    }
}
