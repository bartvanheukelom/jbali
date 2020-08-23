package org.jbali.kotser

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.jbali.util.ReifiedType
import org.jbali.util.StoredExtensionProperty


// TODO this caching, is it effective? do ClassedTypes/KTypes obtained from reified share the identity?
val <T> ReifiedType<T>.serializer: KSerializer<T> by StoredExtensionProperty {
    @Suppress("UNCHECKED_CAST")
    serializer(type) as KSerializer<T>
}
