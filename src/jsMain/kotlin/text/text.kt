package org.jbali.text

actual fun codePointToString(cp: Int) =
        js("String.fromCodePoint(cp)") as String
