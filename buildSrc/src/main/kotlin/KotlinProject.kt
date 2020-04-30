package org.jbali.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.DependencyHandler

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

class KotlinConfigurationContainer(
        private val c: ConfigurationContainer
) : ConfigurationContainer by c {
    val implementation get() = getByName("implementation")
    val runtimeOnly get() = getByName("runtimeOnly")
    val compileOnly get() = getByName("compileOnly")
    val testRuntimeOnly get() = getByName("testRuntimeOnly")
    val testImplementation get() = getByName("testImplementation")
}
