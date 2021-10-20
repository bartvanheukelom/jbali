package org.jbali.math

import com.google.common.collect.DiscreteDomain
import com.google.common.collect.ImmutableRangeSet
import com.google.common.collect.Range
import org.jbali.text.pairSplit
import org.jbali.util.map

class RangeSetParser<N : Comparable<N>>(
    private val parseValue: (String) -> N,
    private val domain: DiscreteDomain<N>
) {
    
    companion object {
        val int = RangeSetParser(String::toInt, DiscreteDomain.integers())
    }
    
    @Suppress("UnstableApiUsage")
    fun parse(input: String): Set<N> =
        ImmutableRangeSet.builder<N>().apply {
            input.splitToSequence(',')
                .map { it.pairSplit("..").fold(
                    { l -> trimParse(l)?.let { n -> Range.singleton(n) } },
                    { r -> r.map(::trimParse).toClosedRange() }
                ) }
                .filterNotNull() // blank entries give null
                .forEach(::add)
        }
            .build().asSet(domain)

    private fun trimParse(v: String): N? =
        v.trim().ifEmpty { null }?.let(parseValue)
    
}
