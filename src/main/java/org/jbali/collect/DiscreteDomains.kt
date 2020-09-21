package org.jbali.collect

import com.google.common.collect.DiscreteDomain

object CharDomain : DiscreteDomain<Char>() {

    override fun next(value: Char): Char? =
            when (value) {
                Char.MAX_VALUE -> null
                else -> value + 1
            }

    override fun distance(start: Char, end: Char): Long =
            end.toLong() - start.toLong()

//    override fun offset(origin: Char, distance: Long): Char =
//            (origin.toLong() + distance).toChar()

    override fun previous(value: Char): Char? =
            when (value) {
                Char.MIN_VALUE -> null
                else -> value - 1
            }

}
