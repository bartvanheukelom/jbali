pluginManagement {
    val kotlinVersion = KotlinVersion(1, 4, 20)
    val kotlinEAPSuffix = null

    plugins {
        id("org.jetbrains.dokka") version "1.4.10.2"
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
        jcenter()
        gradlePluginPortal()

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

commonSettings(
        projectName = "jbali"
)

// ============ shared TODO find way to share this =========== //

fun commonSettings(
        projectName: String,
        submodules: List<String> = emptyList(),
        submodulePath: (String) -> String = { "subs/$it" },
        flatSubmoduleName: Boolean = false
) {

    val meSubPropsFile = file("../$projectName.properties")
    if (meSubPropsFile.exists()) {
        val props =
                java.util.Properties().also { p ->
                    meSubPropsFile.reader().use(p::load)
                }
        val iAmSub = props["isChild"] != "false"
        check(!iAmSub) {
            "$projectName is a submodule of ${file("..")} and should not be used as the Gradle root project."
        }
    }



    rootProject.name = projectName

    submodules.forEach {

        val subPath = submodulePath(it)

        val subDir = File(settingsDir, subPath)
        check(subDir.listFiles()?.isNotEmpty() ?: false) {
            "Submodule $subDir is empty or not a directory. Have you initialized it?"
        }

        if (!flatSubmoduleName) {
            include(":${subPath.replace('/', ':')}")
        } else {
            val subProjectName = ":$it"
            include(subProjectName)
            project(subProjectName).projectDir = subDir
        }

    }
}
