pluginManagement {
    val kotlinVersion = KotlinVersion(1, 3, 72)

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
