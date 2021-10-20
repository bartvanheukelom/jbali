package org.jbali.text

import arrow.core.Either
import arrow.core.left
import arrow.core.right

/**
 * Split this string on the first occurence of [sep] and return the before and after parts, excluding [sep], as the right value.
 * If this string does not contain [sep], return it as the left value.
 * Occurences of [sep] after the first one are ignored, i.e. included in the [Pair.second] verbatim.
 */
// TODO move to common but provide a mpp impl of Either
fun String.pairSplit(sep: String = ","): Either<String, Pair<String, String>> =
    when (val index = indexOf(sep)) {
        -1 -> this.left()
        else -> Pair(substring(0, index), substring(index + sep.length)).right()
    }
