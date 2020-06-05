package org.jbali.kotser

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer


/**
 * Base class for a [KSerializer] for [T] which serializes values
 * by converting them from/to [F] and delegating the actual serialization
 * to the given backend serializer.
 */
// TODO make interface Transformer<T, F>, and a variant of this class which accepts an instance
abstract class TransformingSerializer<T, F>(
        val backend: KSerializer<F>
) : KSerializer<T> {

    /**
     * The descriptor is taken from the backend serializer.
     *
     * If you get an error of the form:
     *    class WhateverSerializer overrides final method getDescriptor
     * You should not apply @Serializer(forClass=Whatever::class) to that class.
     * To make a companion the default serializer, instead apply to main class:
     * @Serializable(with = Whatever.Companion::class)
     */
    final override val descriptor get() = backend.descriptor

    final override fun serialize(encoder: Encoder, value: T) {
        backend.serialize(encoder, transform(value))
    }

    final override fun deserialize(decoder: Decoder): T {
        return detransform(backend.deserialize(decoder))
    }

    abstract fun transform(obj: T): F
    abstract fun detransform(tf: F): T
}
