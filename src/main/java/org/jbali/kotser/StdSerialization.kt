package org.jbali.kotser

import kotlinx.serialization.json.JSON

object StdJSON {
    val plain = JSON().fix()
    val unquoted = JSON(unquoted = true).fix()
    val indented = JSON(indented = true).fix()
    val nonstrict = JSON(strictMode = false).fix()

    private fun JSON.fix() = apply {
        install(dateTimeSerModule)
        install(inetAddressSerModule)
    }
}