package org.jbali.collect

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import java.util.stream.Stream
import kotlin.streams.toList

fun <T> List<T>.toImmutableList() = this as? ImmutableList ?: ImmutableList.copyOf(this)!!
fun <T> Set<T>.toImmutableSet() = this as? ImmutableSet ?: ImmutableSet.copyOf(this)!!
fun <K, V> Map<K, V>.toImmutableMap() = this as? ImmutableMap ?: ImmutableMap.copyOf(this)!!

fun <T> Stream<T>.toImmutableList(): ImmutableList<T> = toList().toImmutableList()
fun <T> Stream<T>.toImmutableSet(): ImmutableSet<T> = ImmutableSet.copyOf(toList())
