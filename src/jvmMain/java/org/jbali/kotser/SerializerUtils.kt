package org.jbali.kotser

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.jbali.util.ReifiedType


// TODO this caching, is it effective? do ClassedTypes/KTypes obtained from reified share the identity?
// TODO is it even needed? new version of serializer() no longer notes that it's heavy - disabled for now because of bug potential
@Suppress("UNCHECKED_CAST")
val <T> ReifiedType<T>.serializer: KSerializer<T> get() = //by StoredExtensionProperty {
    serializer(type) as KSerializer<T>
//}

//fun <T : Any> SerializersModule.getContextualOrDefaultOrNull(value: T): KSerializer<T>? =
//        try {
//            getContextual()
//            getContextualOrDefault(value)
//        } catch (e: Exception) {
//            null
//        }
//
//fun <T : Any> SerialModule.getContextualOrDefaultOrNull(klass: KClass<T>): KSerializer<T>? =
//        try {
//            getContextualOrDefault(klass)
//        } catch (e: Exception) {
//            null
//        }
