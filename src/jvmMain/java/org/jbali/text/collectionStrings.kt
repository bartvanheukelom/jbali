package org.jbali.text

fun Iterable<Any?>.toMessageString(prefix: String) =
    buildString {
        append(prefix)
        forEach {
            append("\n\t")
            append(it.toString())
        }
    }
