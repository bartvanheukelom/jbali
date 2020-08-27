package org.jbali.kotser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

/**
 * Container for some default instances of [Json].
 */
object DefaultJson {

    val stable = Json(JsonConfiguration.Stable)

    val plainConfig =
            JsonConfiguration.Stable.copy(
                    useArrayPolymorphism = true,
                    ignoreUnknownKeys = true
            )

    val read get() = plain
    val readLenient = Json(plainConfig.copy(isLenient = true))

    val plain = Json(plainConfig)
    val plainOmitDefaults = Json(plainConfig.copy(encodeDefaults = false))
    val indented = Json(plainConfig.copy(prettyPrint = true))
    val indentedOmitDefaults = Json(plainConfig.copy(prettyPrint = true, encodeDefaults = false))

}
