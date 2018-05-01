package org.jbali.util

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

data class CachedTransform<out I, out O>(
        val input: I,
        val output: O
) {
    val loadTime = Instant.now()!!
}

class CachingTransform<out I, O>(
        private val input: () -> I,
        private val transform: (I) -> O
) {
    private val cache = AtomicReference<CachedTransform<I,O>>()

    val cached get() = cache.get() ?: null

    operator fun invoke() = cache.updateAndGet { cached ->
        val newIn = input()
        if (cached.input == newIn) cached
        else CachedTransform(newIn, transform(newIn))
    }.output

    fun asSupplier() = Supplier(::invoke)
}
