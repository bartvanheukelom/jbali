package org.jbali.text

fun Iterable<Any?>.toMessageString(
    prefix: String = "",
    sep: String = ""
) =
    buildString {
        append(prefix)
        if (sep.isNotEmpty()) append(sep)
        this@toMessageString.forEach {
            append("\n\t")
            append(it.toString())
        }
    }

infix fun String.showing(items: Iterable<Any?>) = items.toMessageString(this, ":")
infix fun String.showing(items: Map<String, Any?>) =
    items.entries.map { (k, v) ->
        "$k: $v"
    }.toMessageString(this, ":")
