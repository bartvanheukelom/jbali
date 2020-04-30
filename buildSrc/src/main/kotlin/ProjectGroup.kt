package org.jbali.gradle

import org.gradle.api.Project
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class ProjectGroup<T : ProjectWrapper>(
        private val groupProject: Project,
        private val type: KClass<out T>,
        val children: Map<String, T> =
                groupProject.childProjects.mapValues {
                    type.primaryConstructor!!.call(it.value)
                }
) : Set<T> by children.values.toSet() {

    val name get() = groupProject.name

//    fun configure(action: T.() -> Unit): Iterable<T> =
//            configure(children.values, action)

    operator fun div(subProjectName: String): T =
            children.getValue(subProjectName)

    override fun toString() = "ProjectGroup($name)"
    override fun hashCode() = groupProject.hashCode()
    override fun equals(other: Any?) = other is ProjectGroup<*> && other.groupProject == groupProject

}