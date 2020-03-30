package org.jbali.gradle

object JBali {
    const val group = "org.jbali"
    const val aJbali = "jbali"

    const val jbali = "$group:$aJbali"
}

object Arrow {
    const val core = "io.arrow-kt:arrow-core"
}

object Ktor {
    object Server {
        const val core = "io.ktor:ktor-server-core"
    }
    object Client {
        const val cio = "io.ktor:ktor-client-cio"
    }
    const val websockets = "io.ktor:ktor-websockets"
}