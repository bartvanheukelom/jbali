package org.jbali.kotser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerialModule


private val jsonFieldConfig = Json::class.java.getDeclaredField("configuration").also { it.isAccessible = true }
private val jsonFieldContext = Json::class.java.getDeclaredField("context\$1").also { it.isAccessible = true }

/**
 * Create a copy of this [Json] with the [Json.context] returned by [extender],
 * which is called with this [Json]'s context as receiver.
 *
 * Note that the [Json] constructor normally adds [kotlinx.serialization.json.defaultJsonModule] to the context
 * you pass in its constructor. That is the context that [extender] receives.
 *
 * It also means that normally, you cannot create a [Json] with a custom implementation
 * of [SerialModule], but with this function you can. It accomplishes this by injecting the context
 * after construction, through reflection.
 */
fun Json.extendContext(extender: SerialModule.() -> SerialModule): Json {
    val baseConfig = jsonFieldConfig.get(this) as JsonConfiguration
    val baseContext = jsonFieldContext.get(this) as SerialModule
    return Json(configuration = baseConfig)
            .also {
                jsonFieldContext.set(it, baseContext.extender())
            }
}
