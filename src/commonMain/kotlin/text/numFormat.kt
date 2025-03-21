package org.jbali.text


/**
 * Format this number with [ts] as thousands separator.
 */
fun Long.format(ts: String = "_"): String {
    if (this < 0) {
        return "-" + (-this).format(ts)
    }
    
    val digits = toString()
    
    // If there are fewer than 4 digits, nothing to do.
    if (digits.length <= 3) return digits
    
    // Calculate the length of the first group (could be 1 or 2 digits).
    val firstGroupLength = digits.length % 3
    
    val sb = StringBuilder(digits.length + digits.length / 3 * ts.length)
    
    var index = 0
    // Append the first group if it exists.
    if (firstGroupLength != 0) {
        sb.append(digits.substring(0, firstGroupLength))
        index = firstGroupLength
        if (index < digits.length) {
            sb.append(ts)
        }
    }
    // Append the rest of the digits in groups of three.
    while (index < digits.length) {
        sb.append(digits.substring(index, index + 3))
        index += 3
        if (index < digits.length) {
            sb.append(ts)
        }
    }
    return sb.toString()
}
