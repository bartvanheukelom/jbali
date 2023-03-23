package org.jbali.kotser

import kotlinx.serialization.KSerializer
import org.jbali.util.checkReturnType
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.typeOf

inline fun <reified T : Any, reified P> singlePropertySerializer(
        prop: KProperty1<T, P>,
        crossinline construct: (P) -> T
): KSerializer<T> =
        singlePropertySerializer(prop = prop, wrap = construct)

/**
 * Example:
 *
 * ```
 * @Serializable(with = Word.Companion::class)
 * data class Word(val v: String) {
 *     companion object : KSerializer<Word> by singlePropertySerializer(prop = Word::v, wrap = ::Word)
 * }
 * ```
 */
inline fun <reified T : Any, reified P> singlePropertySerializer(
        prop: KProperty1<T, P> = T::class.declaredMemberProperties.single().checkReturnType(),
        serialName: String = typeOf<T>().toString(),
        // TODO can this be generated, either at compile time, or at runtime using information from reflection?
        crossinline wrap: (P) -> T
): KSerializer<T> =
        transformingSerializer(
                serialName = serialName,
                transformer = object : Transformer<T, P> {
                    override fun transform(obj: T) = prop.get(obj)
                    override fun detransform(tf: P) = wrap(tf)
                }
        )
