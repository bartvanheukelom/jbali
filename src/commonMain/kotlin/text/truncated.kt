package org.jbali.text


/**
 * Truncates a string to a specified length, appending an ellipsis if the string exceeds that length.
 *
 * Examples:
 *
 * - `"Hello World".truncated(5) == "Hello"`
 * - `"Hello World".truncated(5, "…") == "Hell…"
 * - `"Hello World".truncated(5, "...") == "He..."`
 * - `"Hello".truncated(10) == "Hello"`
 *
 * @param len The maximum length of the resulting string, including the ellipsis.
 * @param ellipsis The string to append if truncation occurs. Default is an empty string.
 * @return The truncated string with the ellipsis appended if necessary.
 * @throws IllegalArgumentException if the length of [ellipsis] is greater than [len].
 */
fun String.truncated(len: Int, ellipsis: String = ""): String {
    require(ellipsis.length <= len) { "Ellipsis length must be less than or equal to the truncation length" }
    return if (this.length <= len) this else this.substring(0, len - ellipsis.length) + ellipsis
}

/**
 * Shorthand for `truncated(length, "…")`.
 */
fun String.truncatedWithEllipsis(length: Int) = truncated(length, "…")
