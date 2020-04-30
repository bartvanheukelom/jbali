package org.jbali.gradle

import org.gradle.api.Project

val Project.isRoot: Boolean get() =
    this == rootProject

/**
 * Returns the direct children of this project.
 */
val Project.childprojects: Collection<Project> get() =
    childProjects.values

operator fun Project.div(child: String): Project =
        childProjects.getValue(child)
