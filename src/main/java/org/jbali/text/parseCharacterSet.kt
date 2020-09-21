package org.jbali.text

import com.google.common.collect.ImmutableRangeSet
import com.google.common.collect.Range
import org.jbali.collect.CharDomain

/**
 * Parse a regexlike character set string into a [Set] of [Char].
 * `-` is used to specify ranges. To include `-` in the set, list it as first or last character.
 * Example:
 * - `A-Za-z_-` -> `setOf('-', 'A', 'B', ..., 'Z', '_', 'a', ..., 'z')`
 * @throws IllegalArgumentException for syntax errors, or if any input ranges have nonempty overlap
 */
@Suppress("UnstableApiUsage")
fun parseCharacterSet(input: String): Set<Char> =
        ImmutableRangeSet.builder<Char>().apply {

            var state = 0
            var start: Char = Char.MIN_VALUE

            var i = 0

            fun syntaxError(msg: String): Nothing =
                    throw IllegalArgumentException("Syntax error in input \"$input\", char '${input[i]}' at [$i]: $msg")

            while (i < input.length) {
                val c = input[i]

                when (state) {

                    // begin state, is never reentered once left
                    0 -> {
                        if (c == '-') {
                            add(Range.singleton('-'))
                        } else {
                            start = c
                            state = 2
                        }
                    }

                    // normal state, expect a normal char
                    1 -> {
                        if (c == '-') {
                            syntaxError("expected a normal character")
                        } else {
                            start = c
                            state = 2
                        }
                    }

                    // have one normal char, expect dash or next normal char
                    2 -> {
                        if (c == '-') {
                            state = 3
                        } else {
                            add(Range.singleton(start))
                            start = c
                        }
                    }

                    // have start char and dash, expect end char
                    3 -> {
                        when {
                            c == '-' -> {
                                syntaxError("expected end of range started by '$start'")
                            }
                            c <= start -> {
                                syntaxError("end char is not after start char '$start'")
                            }
                            else -> {
                                add(Range.closed(start, c))
                                state = 1
                            }
                        }
                    }

                    else -> throw AssertionError("state $state")
                }

                i++
            }

            // input has ended, handle dangling state
            when (state) {
                // still in base state, can happen if input is empty or all dashes
                0 -> {}
                // normal state, nothing dangling
                1 -> {}
                // have one normal char and it's not going to be a range
                2 -> {
                    add(Range.singleton(start))
                }
                // have start char and dash, but there's not going to be and end, so they are just 2 chars to include
                3 -> {
                    add(Range.singleton(start))
                    add(Range.singleton('-'))
                }

                else -> throw AssertionError("end state $state")
            }

        }.build().asSet(CharDomain)

fun Set<Char>.toConcatString() =
        joinToString("")
