package org.jbali.gradle

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
//import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget

val <T : KotlinCompilation<*>> KotlinOnlyTarget<T>.main: T get() =
        this.compilations.getByName("main")
//    this.compilations["main"]

val <T : KotlinCompilation<*>> KotlinOnlyTarget<T>.test: T get() =
        this.compilations.getByName("test")
//    this.compilations["test"]

class SimpleDependencyHandler(
        val target: KotlinOnlyTarget<*>
) {

    val main = target.main.defaultSourceSet
    val test = target.test.defaultSourceSet

    fun compileAndTest(dependencyNotation: Any) {
        compileOnly(dependencyNotation)
        testImplementation(dependencyNotation)
    }

    fun compileOnly(dependencyNotation: Any) {
        main.dependencies {
            compileOnly(dependencyNotation)
        }
    }

    fun testImplementation(dependencyNotation: Any) {
        test.dependencies {
            implementation(dependencyNotation)
        }
    }

    fun kotlin(module: String, version: String? = null): String =
            "org.jetbrains.kotlin:kotlin-$module${version?.let { ":$version" } ?: ""}"

}

fun KotlinOnlyTarget<*>.simpleDependencies(configure: SimpleDependencyHandler.() -> Unit) {
    SimpleDependencyHandler(this).configure()
}
