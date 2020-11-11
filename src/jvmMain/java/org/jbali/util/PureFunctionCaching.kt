package org.jbali.util

import com.google.common.base.Function
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader


/**
 * Create a cache that stores the result of applying [loader] on
 * objects of type [K], using identity based weak keys.
 *
 * The results are never evicted. It's assumed that [K] is immutable and [loader] is pure.
 *
 * The cache is returned as a function with the same signature as [loader].
 */
fun <K : Any, V : Any> weakKeyLoadingCache(loader: (K) -> V): (K) -> V =
        CacheBuilder.newBuilder()
                .weakKeys()
                .build(CacheLoader.from(Function<K, V> {
                    loader(it!!)
                }))::get

/**
 * Decorate this pure function to cache results using [weakKeyLoadingCache].
 */
fun <K : Any, V : Any> ((K) -> V).identityCached(): (K) -> V =
        weakKeyLoadingCache(this)
