package org.jbali.kotser

import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.modules.*
import kotlinx.serialization.serializer

/**
 * Creates a builder to register subclasses of a given [baseClass] for polymorphic serialization.
 * If [baseSerializer] is not null, registers it as a serializer for [baseClass],
 * which is useful if the base class is serializable itself. To register subclasses,
 * [PolymorphicModuleBuilder.subclass] builder function can be used.
 *
 * If a serializer already registered for the given KClass in the given scope, an [IllegalArgumentException] is thrown.
 * To override registered serializers, combine built module with another using [SerializersModule.overwriteWith].
 *
 * @see PolymorphicSerializer
 */
inline fun <reified Base : Any> SerializersModuleBuilder.polymorphic(
    baseSerializer: KSerializer<Base>? = null,
    builderAction: PolymorphicModuleBuilder<Base>.() -> Unit = {}
) {
    polymorphic(Base::class, baseSerializer, builderAction)
}

///**
// * Registers a serializer for class [T] in the resulting module under the [base class][Base].
// */
//inline fun <Base : Any, reified T : Base> PolymorphicModuleBuilder<Base>.subclass() =
//    subclass(T::class, serializer())
