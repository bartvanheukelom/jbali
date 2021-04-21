@file:OptIn(ExperimentalJsExport::class)
@file:JsExport

package org.jbali.text

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * A single Unicode codepoint, which may require multiple [Char]s for storage.
 */
data class CodePoint(
        val asString: String
) {

    /**
     * From JavaScript: `CodePoint.fromInt(...)`
     */
    // requires @file:JsExport, https://youtrack.jetbrains.com/issue/KT-43313
    @JsName("fromInt")
    constructor(cp: Int) : this(codePointToString(cp))

    init {
        // TODO MPP implementation for codePointCount, or only check it on JVM
//        require(asString.codePointCount(0, asString.length) == 1) { "String must contain 1 code point, '$asString' is invalid" }
    }

    override fun toString() = asString
}
