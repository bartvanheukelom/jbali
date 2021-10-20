package org.jbali.math

import com.google.common.collect.Range

fun <N : Comparable<N>> Pair<N?, N?>.toClosedRange(): Range<N> =
    if (first == null) {
        if (second == null) {
            Range.all()
        } else {
            Range.atMost(second)
        }
    } else {
        if (second == null) {
            Range.atLeast(first)
        } else {
            Range.closed(first, second)
        }
    }
