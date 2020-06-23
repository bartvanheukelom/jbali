package org.jbali.kotser

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.jbali.util.ClassedType


// TODO add caching here, or somewhere.
//      easy if ClassedTypes/KTypes obtained from reified share the identity,
//      more cumbersome if it has a good equals (but what about classloaders),
//      harder if it doesn't.
@Suppress("UNCHECKED_CAST")
val <T : Any> ClassedType<T>.serializer get() =
    serializer(type) as KSerializer<T>
