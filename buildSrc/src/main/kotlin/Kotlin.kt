package org.jbali.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginAware
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

// TODO more specific name and make this name an extension of DependencyHandlerScope
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

    object Coroutines {
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core"
    }

}

object KotlinVersions {
    val V1_3_31 = KotlinVersion(1, 3, 31)
    // TODO ...
    val V1_3_50 = KotlinVersion(1, 3, 50)
    // TODO ...
    val V1_3_71 = KotlinVersion(1, 3, 71)
    val V1_3_72 = KotlinVersion(1, 3, 72)
}

// copied from kotlin-gradle-plugin
fun Project.getKotlinPluginVersion(): String? =
        plugins.asSequence().mapNotNull { (it as? KotlinBasePluginWrapper)?.kotlinPluginVersion }.firstOrNull()

val Project.declaredKotlinVersion: KotlinVersion? get() =
    (gradle as ExtensionAware).extensions.extraProperties["kotlinVersion"] as KotlinVersion?

val Project.kotlinVersion: KotlinVersion? get() =
    (this as ExtensionAware).extensions.extraProperties.properties.getOrPut("kotlinVersion") {
        check(getKotlinPluginVersion() == "$declaredKotlinVersion") {
            "kotlinPluginVersion ${getKotlinPluginVersion()} != kotlinVersion $declaredKotlinVersion"
        }
        declaredKotlinVersion
    } as KotlinVersion?

fun KotlinJvmOptions.enableInlineClasses() {
//    freeCompilerArgs += "-Xinline-classes"
    freeCompilerArgs += "-XXLanguage:+InlineClasses"
}

fun KotlinJvmOptions.inlineClasses() {
    freeCompilerArgs += "-Xinline-classes"
}

enum class Experimentals(val featureName: String) {
    Contracts("kotlin.contracts.ExperimentalContracts"),
    Experimental("kotlin.Experimental"),
    RequiresOptIn("kotlin.RequiresOptIn")
}

fun KotlinJvmOptions.use(feature: Experimentals) {
    useExperimental(feature.featureName)
}

fun KotlinJvmOptions.useExperimental(feature: String) {
    freeCompilerArgs += "-Xuse-experimental=$feature"
}

fun KotlinJvmOptions.optIn(feature: Experimentals) {
    freeCompilerArgs += "-Xopt-in=${feature.featureName}"
}

// doesn't appear like it can be used, will complain "this class can only be used as..."
inline fun <reified C : Any> KotlinJvmOptions.useExperimental() {
    freeCompilerArgs += "-Xuse-experimental=${C::class.qualifiedName}"
}

enum class KotlinCompilerPlugin {
    jpa,
    spring,
    serialization;

    val id = "org.jetbrains.kotlin.plugin.$name"

    fun applyTo(project: PluginAware) {
        project.pluginManager.apply(id)
    }
}
