package org.jbali.json2

import java.lang.reflect.Method

// TODO is there a non-reflection way to call an internal function?
//      or just copy the relevant code, with correct licensing.
private val kotserPrintQuoted: Method =
        Class.forName("kotlinx.serialization.json.internal.StringOpsKt")
                .getDeclaredMethod("printQuoted", StringBuilder::class.java, String::class.java)
                .apply {
                    isAccessible = true
                }

/**
 * Append the given string, quoted and escaped as a JSON string literal.
 */
fun StringBuilder.appendJsonQuoted(str: String) {
    kotserPrintQuoted.invoke(null, this, str)
}

/**
 * Returns this string, quoted and escaped as a JSON string literal.
 *
 * Example: `foo is not a "bar", right?` becomes `"foo is not a \"bar\", right"`
 */
fun String.jsonQuote() =
        buildString {
            appendJsonQuoted(this@jsonQuote)
        }
