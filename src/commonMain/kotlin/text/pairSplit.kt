package org.jbali.text


/**
 * Split this string on the first occurence of [sep] and return the before and after parts, excluding [sep].
 * If this string does not contain [sep], returns `Pair(this, null)`.
 */
fun String.pairSplitLeft(sep: String = ","): Pair<String, String?> =
    when (val index = indexOf(sep)) {
        -1 -> Pair(this, null)
        else -> Pair(substring(0, index), substring(index + sep.length))
    }

/**
 * Split this string on the first occurence of [sep] and return the before and after parts, excluding [sep].
 * If this string does not contain [sep], returns `Pair(null, this)`.
 */
fun String.pairSplitRight(sep: String = ","): Pair<String?, String> =
    when (val index = indexOf(sep)) {
        -1 -> Pair(null, this)
        else -> Pair(substring(0, index), substring(index + sep.length))
    }


/**
 * Returns both parts joined by [sep].
 * If the second (right) part is `null`, only the first (left) part is returned, without [sep].
 */
fun Pair<String, String?>.pairJoinLeft(sep: String = ",") =
    if (second == null) {
        first
    } else {
        "$first$sep$second"
    }

/**
 * Returns both parts joined by [sep].
 * If the first (left) part is `null`, only the second (right) part is returned, without [sep].
 */
fun Pair<String?, String>.pairJoinRight(sep: String = ",") =
    if (first == null) {
        second
    } else {
        "$first$sep$second"
    }
