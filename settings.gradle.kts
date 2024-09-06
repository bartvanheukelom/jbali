pluginManagement {
    val kotlinVersion = KotlinVersion(1, 4, 21)
    val kotlinEAPSuffix = null

    plugins {
        id("org.jetbrains.dokka") version "1.4.20"
    }

    // ============ shared TODO find way to share this =========== //

    val kotlinVersionString = "$kotlinVersion${kotlinEAPSuffix ?: ""}"
    @Suppress("SENSELESS_COMPARISON")
    val kotlinEAP = kotlinEAPSuffix != null

    (gradle as ExtensionAware).extra.let { e ->

        // TODO make wrapper class with suffix included
        // this one preserves it as KotlinVersion
        check(!e.has("kotlinVersion"))
        e["kotlinVersion"] = kotlinVersion
        // but this one includes the suffix
        check(!e.has("kotlinVersionString"))
        e["kotlinVersionString"] = kotlinVersionString

        check(!e.has("kotlinEAP"))
        e["kotlinEAP"] = kotlinEAP

    }

    repositories {
        mavenCentral()
        gradlePluginPortal()
        
        // as of 2024-09-06, it appears dokka 1.4.20 has disappeared from the gradle plugin portal
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev/") }

        if (kotlinEAP) {
            maven("https://dl.bintray.com/kotlin/kotlin-eap")
            maven("https://kotlin.bintray.com/kotlinx")
        }
    }

    plugins {
        listOf(
                "org.jetbrains.kotlin.jvm",
                "org.jetbrains.kotlin.multiplatform",
                "org.jetbrains.kotlin.plugin.serialization"
        ).forEach {
            id(it) version kotlinVersionString
        }
    }

}

rootProject.name = "jbali"

includeBuild("gradle-tools")
