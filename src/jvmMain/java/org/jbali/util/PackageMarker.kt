package org.jbali.util

/**
 * Base class for an object that can be used as reference to a package.
 * Example:
 *
 *     package com.example.foo
 *     object Package : PackageMarker()
 *
 *     ...
 *
 *     package org.anything
 *
 *     assertEquals("com.example.foo", com.example.foo.Package.name)
 */
abstract class PackageMarker {

    val name = javaClass.`package`.name

    override fun toString() = "package $name"

    fun resource(resName: String) =
            javaClass.getResource(resName) ?:
            throw IllegalArgumentException("Resource ${name}.$resName not found")
}
