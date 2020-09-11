package org.jbali.kotser

import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerialModule
import kotlinx.serialization.modules.getContextualOrDefault
import kotlinx.serialization.serializer
import org.jbali.util.ReifiedType
import org.jbali.util.StoredExtensionProperty
import kotlin.reflect.KClass


// TODO this caching, is it effective? do ClassedTypes/KTypes obtained from reified share the identity?
val <T> ReifiedType<T>.serializer: KSerializer<T> by StoredExtensionProperty {
    @Suppress("UNCHECKED_CAST")
    serializer(type) as KSerializer<T>
}

@ImplicitReflectionSerializer
fun <T : Any> SerialModule.getContextualOrDefaultOrNull(value: T): KSerializer<T>? =
        try {
            getContextualOrDefault(value)
        } catch (e: Exception) {
            null
        }

@ImplicitReflectionSerializer
fun <T : Any> SerialModule.getContextualOrDefaultOrNull(klass: KClass<T>): KSerializer<T>? =
        try {
            getContextualOrDefault(klass)
        } catch (e: Exception) {
            null
        }
