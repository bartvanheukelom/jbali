package org.jbali.kotser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

object DefaultJson {

    val plainConfig =
            JsonConfiguration.Stable.copy(
                    useArrayPolymorphism = true
            )

    val plain = Json(plainConfig)
    val indented = Json(plainConfig.copy(prettyPrint = true))

}