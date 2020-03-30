package org.jbali.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

object Kotlin {
    private const val modulePrefix = "org.jetbrains.kotlin:kotlin-"

    object StdLib {
        const val common = "${modulePrefix}stdlib-common"
        const val js = "${modulePrefix}stdlib-js"
        const val jvm = "${modulePrefix}stdlib"
        const val jdk8 = "${modulePrefix}stdlib-jdk8"
    }

    object Test {
        const val annotationsCommon = "${modulePrefix}test-annotations-common"
        const val common = "${modulePrefix}test-common"
        const val js = "${modulePrefix}test-js"
        const val jvm = "${modulePrefix}test"
        const val junit = "${modulePrefix}test-junit"
    }

    const val reflect = "${modulePrefix}reflect"

}

object KotlinX {
    object SerializationRuntime {
        const val common = "org.jetbrains.kotlinx:kotlinx-serialization-runtime-common"
        const val js = "org.jetbrains.kotlinx:kotlinx-serialization-runtime-js"
        const val jvm = "org.jetbrains.kotlinx:kotlinx-serialization-runtime"
    }
}

object KotlinVersions {
    val V1_3_31 = KotlinVersion(1, 3, 31)
    // TODO ...
    val V1_3_50 = KotlinVersion(1, 3, 50)
    // TODO ...
    val V1_3_71 = KotlinVersion(1, 3, 71)
}

// copied from kotlin-gradle-plugin
fun Project.getKotlinPluginVersion(): String? =
        plugins.asSequence().mapNotNull { (it as? KotlinBasePluginWrapper)?.kotlinPluginVersion }.firstOrNull()

val Project.kotlinVersion: KotlinVersion? get() =
    (this as ExtensionAware).extensions.extraProperties.properties.getOrPut("kotlinVersion") {

        val declaredVersion = (gradle as ExtensionAware).extensions.extraProperties["kotlinVersion"] as KotlinVersion?
        val pluginVersion = getKotlinPluginVersion()

        check(pluginVersion == "$declaredVersion") {
            "kotlinPluginVersion $pluginVersion != kotlinVersion $declaredVersion"
        }

        declaredVersion

    } as KotlinVersion?

fun KotlinJvmOptions.enableInlineClasses() {
//    freeCompilerArgs += "-Xinline-classes"
    freeCompilerArgs += "-XXLanguage:+InlineClasses"
}

enum class Experimentals(val featureName: String) {
    Contracts("kotlin.contracts.ExperimentalContracts"),
    Experimental("kotlin.Experimental")
}

fun KotlinJvmOptions.use(feature: Experimentals) {
    useExperimental(feature.featureName)
}

fun KotlinJvmOptions.useExperimental(feature: String) {
    freeCompilerArgs += "-Xuse-experimental=$feature"
}

// doesn't appear like it can be used, will complain "this class can only be used as..."
inline fun <reified C : Any> KotlinJvmOptions.useExperimental() {
    freeCompilerArgs += "-Xuse-experimental=${C::class.qualifiedName}"
}
