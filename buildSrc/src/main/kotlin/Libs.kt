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
        const val netty = "io.ktor:ktor-server-netty"
    }
    object Auth {
        const val core = "io.ktor:ktor-auth"
        const val jwt = "io.ktor:ktor-auth-jwt"
    }
    object Client {
        const val cio = "io.ktor:ktor-client-cio"
        object Logging {
            const val jvm = "io.ktor:ktor-client-logging-jvm"
        }
        object Serialization {
            const val core = "io.ktor:ktor-client-serialization"
            const val jvm = "io.ktor:ktor-client-serialization-jvm"
        }
    }
    const val websockets = "io.ktor:ktor-websockets"
}