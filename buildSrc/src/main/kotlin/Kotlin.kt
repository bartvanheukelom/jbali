package org.jbali.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.DependencyHandler
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

/**
 * [https://github.com/JetBrains/kotlin/blob/master/ChangeLog.md]
 */
object KotlinVersions {

    // TODO earlier

    val V1_3_0  = KotlinVersion(1, 3, 0)

    val V1_3_10 = KotlinVersion(1, 3, 10)
    val V1_3_11 = KotlinVersion(1, 3, 11)

    val V1_3_20 = KotlinVersion(1, 3, 20)
    val V1_3_21 = KotlinVersion(1, 3, 21)

    val V1_3_30 = KotlinVersion(1, 3, 30)
    val V1_3_31 = KotlinVersion(1, 3, 31)

    val V1_3_40 = KotlinVersion(1, 3, 40)
    val V1_3_41 = KotlinVersion(1, 3, 41)

    val V1_3_50 = KotlinVersion(1, 3, 50)

    val V1_3_60 = KotlinVersion(1, 3, 60)
    val V1_3_61 = KotlinVersion(1, 3, 61)

    val V1_3_70 = KotlinVersion(1, 3, 70)
    val V1_3_71 = KotlinVersion(1, 3, 71)
    val V1_3_72 = KotlinVersion(1, 3, 72)
}

class KotlinConfigurationContainer(
        private val c: ConfigurationContainer
) : ConfigurationContainer by c {
    val implementation get() = getByName("implementation")
    val runtimeOnly get() = getByName("runtimeOnly")
    val compileOnly get() = getByName("compileOnly")
    val testRuntimeOnly get() = getByName("testRuntimeOnly")
    val testImplementation get() = getByName("testImplementation")
}

interface KotlinProject : Project {

    val nativeConfigurations: ConfigurationContainer

    override fun getConfigurations() = KotlinConfigurationContainer(nativeConfigurations)

    val DependencyHandler.implementation: Configuration get() = configurations.implementation
    val DependencyHandler.runtimeOnly: Configuration get() = configurations.runtimeOnly
    val DependencyHandler.compileOnly: Configuration get() = configurations.compileOnly
    val DependencyHandler.testRuntimeOnly: Configuration get() = configurations.testRuntimeOnly
    val DependencyHandler.testImplementation: Configuration get() = configurations.testImplementation

}

open class KotlinProjectWrapper(native: Project) : ProjectWrapper(native), KotlinProject {
    override val nativeConfigurations get() = native.configurations
    override fun getConfigurations() = super<KotlinProject>.getConfigurations()
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
