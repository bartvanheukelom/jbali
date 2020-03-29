package org.jbali.gradle

import org.gradle.api.Project

class LibDependencyScope(
        val project: Project
) {

    fun jcenter() {
        if (project.isRoot) {
            project.repositories.jcenter()
        }
    }

    // TODO delegate to the original kotlin function, but requires plugin availability
    fun kotlin(module: String, version: String? = null): String =
            "org.jetbrains.kotlin:kotlin-$module${version?.let { ":$version" } ?: ""}"

    fun add(config: String, dependency: String, defaultVersion: String? = null) {

        val dep =
                buildString {
                    append(dependency)
                    if (project.isRoot && defaultVersion != null) {
                        append(":$defaultVersion")
                    }
                }

        addDep(config, dep)

    }

    private fun addDep(config: String, dep: Any) {
        val configs =
                when (config) {
                    "compileAndTest" ->
                        listOf(
                                "compileOnly",
                                "testImplementation"
                        )
                    else -> listOf(config)
                }

        configs.forEach {
            project.dependencies.add(it, dep)
        }
    }

    /**
     * Add the given dependency to the `compileOnly` and `testImplementation` configurations.
     */
    fun compileAndTest(dependency: String, defaultVersion: String? = null) {
        add("compileAndTest", dependency, defaultVersion)
    }

    fun compileAndTest(project: Project) {
        addDep("compileAndTest", project)
    }

    fun testImplementation(dependency: String, defaultVersion: String? = null) {
        add("testImplementation", dependency, defaultVersion)
    }

}

/**
 * Configure the dependencies for this library project.
 * Dependency versions specified inside this block only apply if this library
 * is the root project of the current build.
 * Otherwise, they should be specified using constraints by the containing project.
 */
fun Project.libdependencies(config: LibDependencyScope.() -> Unit) {
    LibDependencyScope(this).config()
}
