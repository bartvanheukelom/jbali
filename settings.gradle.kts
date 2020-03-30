pluginManagement {
    val kotlinVersion = KotlinVersion(1, 3, 71)

    // ============ shared TODO find way to share this =========== //

    (gradle as ExtensionAware).extra["kotlinVersion"] = kotlinVersion
    val kserLegacyPlugin = !kotlinVersion.isAtLeast(1, 3, 50)

    repositories {
        // for kser
        if (kserLegacyPlugin) {
            maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
        }
        gradlePluginPortal()
    }

    plugins {
        listOf(
                "org.jetbrains.kotlin.jvm",
                "org.jetbrains.kotlin.multiplatform",
                "org.jetbrains.kotlin.plugin.serialization"
        ).forEach {
            id(it) version "$kotlinVersion"
        }
    }

    // https://youtrack.jetbrains.com/issue/KT-27612
    if (!kotlinVersion.isAtLeast(1, 3, 50)) {
        resolutionStrategy {
            eachPlugin {
                when (requested.id.id) {
                    "org.jetbrains.kotlin.plugin.serialization" -> "org.jetbrains.kotlin:kotlin-serialization:$kotlinVersion"
                    else -> null
                }?.let(::useModule)
            }
        }
    }
}

rootProject.name = "jbali"
