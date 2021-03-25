package org.jbali.ktor

import io.ktor.util.*

operator fun <T : Any> Attributes.set(key: AttributeKey<T>, value: T) {
    put(key, value)
}
