package org.jbali.text

operator fun String.div(rhs: String) =
        // TODO efficient implementation that preallocates char array
        buildString {
            append(this@div)
            append('/')
            append(rhs)
        }

actual fun codePointToString(cp: Int) =
        String(intArrayOf(cp), 0, 1)
