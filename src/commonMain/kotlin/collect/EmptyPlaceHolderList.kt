package org.jbali.collect

object EmptyPlaceHolderList : List<Nothing> by emptyList() {
    override fun toString() = "[...]"
}

/**
 * Returns an empty list whose [toString] is `[...]`,
 * as opposed to the `[]` of e.g. [emptyList].
 */
fun placeHolderList(): List<Nothing> =
    EmptyPlaceHolderList
