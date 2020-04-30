package org.jbali.gradle

import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.jbali.gradle.kotlinVersion

/**
 * Sets the project [group] and checks its [name] (which can only be set in `settings.gradle*`).
 *
 * If [acceptableKotlinVersions] is given, checks whether the actual Kotlin version is one of them.
 *
 * @throws IllegalStateException if any of these conditions fail.
 */
fun Project.initKotlinProject(
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

    tasks.withType(Jar::class.java).forEach {
        it.archiveFileName.set("$group.$name.jar")
    }

}

@Deprecated(message = "renamed", replaceWith = ReplaceWith("initKotlinProject(group, name, acceptableKotlinVersions)"))
fun Project.initProject(
        group: String,
        name: String,
        acceptableKotlinVersions: Set<KotlinVersion>? = null
) = initKotlinProject(group = group, name = name, acceptableKotlinVersions = acceptableKotlinVersions)
