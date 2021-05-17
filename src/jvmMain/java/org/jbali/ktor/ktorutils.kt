package org.jbali.ktor

import io.ktor.http.*
import io.ktor.util.*

operator fun <T : Any> Attributes.set(key: AttributeKey<T>, value: T) {
    put(key, value)
}

operator fun Headers.plus(rhs: Map<String, String>) =
    HeadersBuilder()
        .apply {
            appendAll(this@plus)
            rhs.forEach(::append)
        }
        .build()
