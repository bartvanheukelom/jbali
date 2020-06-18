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
                    useArrayPolymorphism = true
            )

    val read get() = plain

    val plain = Json(plainConfig)
    val indented = Json(plainConfig.copy(prettyPrint = true))

}
