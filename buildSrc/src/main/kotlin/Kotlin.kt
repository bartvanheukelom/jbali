package org.jbali.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginAware
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonToolOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions

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

fun KotlinCommonToolOptions.enableInlineClasses() {
//    freeCompilerArgs += "-Xinline-classes"
    freeCompilerArgs += "-XXLanguage:+InlineClasses"
}

fun KotlinCommonToolOptions.inlineClasses() {
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
