package org.jbali.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyConstraintHandler
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.deprecation.DeprecatableConfiguration
import org.gradle.jvm.tasks.Jar
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import java.io.File
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.primaryConstructor


/**
 * Add the given dependency to the `compileOnly` and `testImplementation` configurations.
 */
fun DependencyHandler.compileAndTest(dependencyNotation: Any) {
   add("compileOnly", dependencyNotation)
   add("testImplementation", dependencyNotation)
}

//fun <T> Iterable<T>.configure(action: T.() -> Unit) =
//        forEach {
//            it.action()
//        }

operator fun File.div(child: String) =
        File(this, child)

//val Project.childproject get() =
//    object : ReadOnlyProperty<Project, Project?> {
//        override fun getValue(thisRef: Project, property: KProperty<*>) =
//                thisRef.childProjects[property.name]
//    }

val Configuration.deprecatedForDeclaration: Boolean get() =
        when (this) {
            is DeprecatableConfiguration -> this.declarationAlternatives != null
            else -> false
        }

//    private fun constrain(dependency: String, constraint: String) {
//        project.configurations.forEach { conf ->
//            if (!conf.deprecatedForDeclaration) {
//                project.dependencies.constraints.add(conf.name, "$dependency:$constraint")
//            }
//        }
//    }

// TODO make this a task type as well
fun Project.bash(script: String) {
    this.exec { s: ExecSpec ->
        s.commandLine("bash", "-c", "set -e; $script")
    }
}

