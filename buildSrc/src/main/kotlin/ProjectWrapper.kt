package org.jbali.gradle

import org.gradle.api.Project

abstract class ProjectWrapper(val native: Project) : Project by native {
    override fun toString() = "${this.javaClass}($native)"
    override fun hashCode() = native.hashCode()
    override fun equals(other: Any?) =
            other is Project && other.path == path
}