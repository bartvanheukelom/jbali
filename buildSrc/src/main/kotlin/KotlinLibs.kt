package org.jbali.gradle


// TODO more specific name and make this name an extension of DependencyHandlerScope
object Kotlin {
    private const val modulePrefix = "org.jetbrains.kotlin:kotlin-"

    object StdLib {
        const val common = "${modulePrefix}stdlib-common"
        const val js = "${modulePrefix}stdlib-js"
        const val jvm = "${modulePrefix}stdlib"
        const val jdk8 = "${modulePrefix}stdlib-jdk8"
    }

    object Test {
        const val annotationsCommon = "${modulePrefix}test-annotations-common"
        const val common = "${modulePrefix}test-common"
        const val js = "${modulePrefix}test-js"
        const val jvm = "${modulePrefix}test"
        const val junit = "${modulePrefix}test-junit"
    }

    const val reflect = "${modulePrefix}reflect"

}

object KotlinX {
    object SerializationRuntime {
        const val common = "org.jetbrains.kotlinx:kotlinx-serialization-runtime-common"
        const val js = "org.jetbrains.kotlinx:kotlinx-serialization-runtime-js"
        const val jvm = "org.jetbrains.kotlinx:kotlinx-serialization-runtime"
    }

    object Coroutines {
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core"
    }

}
