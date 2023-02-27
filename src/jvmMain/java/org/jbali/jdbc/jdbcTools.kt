package org.jbali.jdbc

import java.sql.ResultSet

fun ResultSet.asSequence(): Sequence<ResultSet> = sequence {
    while (next()) {
        yield(this@asSequence)
    }
}

fun <T> ResultSet.map(transform: (ResultSet) -> T): List<T> =
    asSequence().map(transform).toList()
