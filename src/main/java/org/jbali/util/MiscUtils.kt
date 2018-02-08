package org.jbali.util

import arrow.core.Either

/**
 * Return a function 'f_once' that will invoke f,
 * only the first time f_once is invoked.
 *
 * Using f_once from multiple threads, or throwing from f,
 * triggers undefined behaviour.
 */
fun onceFunction(f: () -> Unit): () -> Unit {
    var invoked = false
    return {
        if (!invoked) {
            invoked = true
            f()
        }
    }
}

fun <A,B> Either<A,B>.any() = fold({it},{it})
