package org.jbali.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyConstraintHandler
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.deprecation.DeprecatableConfiguration
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper


/**
 * Add the given dependency to the `compileOnly` and `testImplementation` configurations.
 */
fun DependencyHandler.compileAndTest(dependencyNotation: Any) {
   add("compileOnly", dependencyNotation)
   add("testImplementation", dependencyNotation)
}

val Project.isRoot: Boolean get() =
        this == rootProject

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

fun Project.bash(script: String) {
    this.exec {
        val s = this as ExecSpec
        s.commandLine("bash", "-c", "set -e; $script")
    }
}

/**
 * Sets the project [group] and checks its [name] (which can only be set in `settings.gradle*`).
 *
 * If [acceptableKotlinVersions] is given, checks whether the actual Kotlin version is one of them.
 *
 * @throws IllegalStateException if any of these conditions fail.
 */
fun Project.initProject(
        group: String,
        name: String,
        acceptableKotlinVersions: Set<KotlinVersion>? = null
) {

    // get kotlinVersion to check consistency between declared and plugin version
    val kv = kotlinVersion

    if (acceptableKotlinVersions != null) {
        check(kv in acceptableKotlinVersions) {
            "kotlinVersion $kv is not in acceptableKotlinVersions $acceptableKotlinVersions"
        }
    }

    this.group = group

    check(this.name == name) {
        "Name of $this should be '$name'. The project name is taken from the directory name, but can be overriden in `settings.gradle.*`."
    }

}
