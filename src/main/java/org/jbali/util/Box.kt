package org.jbali.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import org.jbali.kotser.Transformer
import org.jbali.kotser.transformingSerializer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Very simple wrapper around any value.
 * Can for instance be used to have nested nullability, i.e. a `Box<Int?>?` to distinguish between:
 * - `null`
 * - `Box(null)`
 * - `Box(12)`
 *
 * A box is equal to another box if the contents are also equal. This is in contrast to [ObjectIdentity].
 */
@Serializable(with = Box.Serializer::class)
data class Box<out T>(val contents: T) : java.io.Serializable {

    // this version complements .boxed()
    fun unboxed() = contents

    class Serializer<T>(contentSerializer: KSerializer<T>) :
            KSerializer<Box<T>> by transformingSerializer<Box<T>, List<T>>(
                    backend = contentSerializer.list,
                    transformer = object : Transformer<Box<T>, List<T>> {
                        override fun transform(obj: Box<T>): List<T> =
                                listOf(obj.contents)

                        override fun detransform(tf: List<T>): Box<T> =
                                Box(tf.single())
                    }
            )

    companion object {
        const val serialVersionUID = 1L
    }

}

fun <T> T.boxed() = Box(this)

/**
 * If `this` is a non-null [Box], calls the specified function [block] with the [Box.contents] as its argument and returns its result.
 * Otherwise returns `null`.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T, R> Box<T>?.letUnboxed(block: (T) -> R): R? {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
//        returnsNotNull() implies (this@letUnboxed != null) // TODO
    }
    return this?.let { block(it.contents) }
}

// just a test for the TODO contract above
//fun xxxxx(b: Box<String>?) {
//    val x  = b.letUnboxed { 12 }
//    if (x != null) {
//        print(b.contents)
//    }
//}