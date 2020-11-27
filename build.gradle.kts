import org.jbali.gradle.*
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
}

// check gradle version, for when not running with the included wrapper (e.g. included in another project)
val supportedGradleVersions = setOf(
        "6.2.2",
        "6.7.1"
)
check(org.gradle.util.GradleVersion.current().version in supportedGradleVersions) {
    "This build script is untested with Gradle version ${org.gradle.util.GradleVersion.current()}. Tested versions are $supportedGradleVersions"
}

initKotlinProject(
        group = JBali.group,
        name = JBali.aJbali,
        acceptableKotlinVersions = setOf(
                KotlinVersions.V1_4_20
        ),
        acceptableKotlinVersionStrings = setOf("1.4.20-RC")
)

// TODO centralize
check(kotlinVersionString == KotlinCompilerVersion.VERSION)


// determine which platforms to include
val jsOnly = System.getProperty("$name.jsOnly") == "true"
val doJvm = System.getProperty("$name.doJvm") == "true" || !jsOnly
val doJs = System.getProperty("$name.doJs") != "false" || jsOnly

commonConfig()
if (doJvm) jvmConfig()
if (doJs) jsConfig()

if (project.isRoot) {
    rootProjectConfig()
}

fun commonConfig() {

    kotlin {

        sourceSets {

            commonMain {
                dependencies {
                    implementation(KotlinX.Serialization.json)
                }
            }

            commonTest {
                dependencies {
                    implementation(kotlin("test-common"))
                    implementation(kotlin("test-annotations-common"))
                }
            }

        }
    }

    tasks.withType<KotlinCompile<*>> {
        kotlinOptions {
            inlineClasses()
            use(Experimentals.RequiresOptIn)
        }
    }

}

fun jvmConfig() {

    kotlin {

        jvm {

            withJava()

            sourceSets {
                val jvmMain by existing {
                    dependencies {
                        implementation(Kotlin.reflect)

                        implementation(Arrow.core)

                        implementation("com.google.guava:guava")
                        implementation("org.slf4j:slf4j-api")

                        implementation("commons-codec:commons-codec")
                        implementation("org.apache.httpcomponents:httpclient")
                        implementation("org.apache.httpcomponents:httpcore")
                        implementation("org.threeten:threeten-extra")

                        // for test library code used in other projects' actual tests
                        compileOnly(Kotlin.Test.jvm)

                        // TODO extract these to own modules or features, see
                        //      https://docs.gradle.org/current/userguide/feature_variants.html
                        compileOnly(Ktor.Server.core)
                        compileOnly(Ktor.websockets)
                        compileOnly(Ktor.Client.cio)
                        compileOnly("io.ktor:ktor-serialization")
                        compileOnly("com.google.code.gson:gson")
                        compileOnly("org.apache.activemq:activemq-client")

                    }
                }
                val jvmTest by existing {
                    dependencies {
                        implementation(Kotlin.Test.jvm)
                        implementation(Kotlin.Test.junit)
                        implementation("org.slf4j:jul-to-slf4j")
                        implementation("org.slf4j:jcl-over-slf4j")
                        implementation("org.slf4j:slf4j-simple")
                        implementation("junit:junit")

                        // add those dependencies that are not transitively included
                        configurations
                                .getByName(jvmMain.get().compileOnlyConfigurationName)
                                .dependencies.forEach {
                                    implementation(it)
                                }
                    }
                }

            }

        }
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
    }

}

fun jsConfig() {

    kotlin {

        js(compiler = IR) {

            // TODO what exactly is the difference between these? and can't I do both or something universal?
            // for now it seems that the "nodejs" code can run fine in a browser, and adding the browser environment is just making things like testing complicated
//            browser()
            nodejs()

            binaries.library()

            // TODO source map https://youtrack.jetbrains.com/issue/KT-39447
            // TODO get/pack kotlin stdlib types
            // TODO better module name?

        }

        sourceSets {
            val jsTest by existing {
                dependencies {
                    implementation(kotlin("test-js"))
                }
            }
        }

    }

    val jsMainClasses by tasks.existing
    val jsTestClasses by tasks.existing

    val jsPublicPackageJson by tasks.existing
    val jsProductionLibraryCompileSync by tasks.existing

    // TODO find out which built-in task is equivalent (surely one is?)
    val jsBuildPackage by tasks.registering {
        dependsOn(jsPublicPackageJson, jsProductionLibraryCompileSync)
    }

    // ------------------------ jsUsageTest ------------------------- //
    //
    // Test whether the exported module is usable as a library in an
    // example TypeScript project.
    //
    // -------------------------------------------------------------- //

//    val jsUsageTestDir = "js/usage_test"
//
//    tasks {
//
//        fun Task.common() {
//            group = "js usage test"
//        }
//
//        val jsUsageTestNpmInstall by registering(Exec::class) {
//            common()
//
//            workingDir(jsUsageTestDir)
//            commandLine("npm", "i")
//
//            inputs.file("$jsUsageTestDir/package.json")
//            outputs.file("$jsUsageTestDir/package-lock.json")
//            outputs.dir("$jsUsageTestDir/node_modules")
//        }
//
//        val jsUsageTestTsc by registering(Exec::class) {
//            common()
//
//            dependsOn(
//                    jsBuildPackage,
//                    jsUsageTestNpmInstall
//            )
//
//            inputs.dir("$jsUsageTestDir/node_modules")
//            inputs.dir("$jsUsageTestDir/src")
//            inputs.file("$jsUsageTestDir/tsconfig.json")
//
//            outputs.file("$jsUsageTestDir/index.js")
//            outputs.dir("$jsUsageTestDir/out")
//
//
//            workingDir(jsUsageTestDir)
//            commandLine("tsc")
//        }
//
//        val jsUsageTest by registering(Exec::class) {
//            common()
//
//            dependsOn(jsUsageTestTsc)
//
//            inputs.dir("$jsUsageTestDir/node_modules")
//            inputs.dir("$jsUsageTestDir/out")
//            outputs.upToDateWhen { true }
//
//            workingDir("$jsUsageTestDir/out")
//            commandLine("node", "--enable-source-maps", ".")
//        }
//
//        val check by existing {
//            dependsOn(jsUsageTest)
//        }
//
//    }

}

/**
 * Configuration that only applies if this project is built
 * standalone, i.e. is the root project.
 * // TODO should be in a separate file, but that's harder with KTS than with Groovy.
 */
fun rootProjectConfig() {

    val ksr = "1.0.1"
    val ktor = "1.4.1"
    val slf4j = "1.7.30"

    val standAloneVersions = mutableListOf(
            "${KotlinX.Serialization.json}:$ksr"
    )

    val forbiddenDependencies = listOf(
            // these conflict with the new KotlinX.Serialization.json,
            // but somebody may try to pull them in transitively
            KotlinX.SerializationRuntime.common,
            KotlinX.SerializationRuntime.jvm,
            KotlinX.SerializationRuntime.js
    )

    if (doJvm) {
        standAloneVersions += listOf(

                "org.slf4j:slf4j-api:$slf4j",
                "org.slf4j:jul-to-slf4j:$slf4j",
                "org.slf4j:jcl-over-slf4j:$slf4j",
                "org.slf4j:slf4j-simple:$slf4j",

                "${Ktor.Server.core}:$ktor",
                "${Ktor.websockets}:$ktor",
                "${Ktor.Client.cio}:$ktor",
                "io.ktor:ktor-serialization:$ktor",

                "${Arrow.core}:0.10.5",

                "com.google.guava:guava:29.0-jre",
                "org.jetbrains:annotations:15.0",
                "com.google.code.gson:gson:2.8.6",
                "org.apache.activemq:activemq-client:5.11.1",
                "commons-codec:commons-codec:1.10",
                "org.apache.httpcomponents:httpclient:4.4",
                "org.apache.httpcomponents:httpcore:4.4",
                "commons-codec:commons-codec:1.10",
                "org.threeten:threeten-extra:1.5.0"
        )

        val javaVersion = JavaVersion.VERSION_1_8

        tasks.withType<KotlinJvmCompile> {
            kotlinOptions {
                jvmTarget = javaVersion.toString()
            }
        }

        tasks.withType<JavaCompile> {
            sourceCompatibility = javaVersion.toString()
            targetCompatibility = sourceCompatibility
        }

    }

    configure(allprojects) {
        repositories {
            jcenter()
        }
    }

    gradle.projectsEvaluated {

        allprojects {

            configurations.forEach { conf ->

                conf.resolutionStrategy {

                    // TODO would be nice but is quite strict
//                    failOnVersionConflict()
//                    @Suppress("UnstableApiUsage")
//                    failOnNonReproducibleResolution()

                    eachDependency {
                        val dep = requested.group + ":" + requested.name
                        require(dep !in forbiddenDependencies) {
                            "$dep in forbiddenDependencies"
                        }
                    }

                    // substitute submodules
                    dependencySubstitution {
//                        submodules.forEach { sub ->
//                            val moduleName = "${sub.group}:${sub.name}"
//                            val projectPath = sub.path
////                        println("substitute $moduleName -> $projectPath")
//                            substitute(module(moduleName))
//                                    .with(project(projectPath))
//                        }
                    }
                }

                // install gradle dependency constraints everywhere
                standAloneVersions.forEach {
                    dependencies.constraints.add(conf.name, "$it!!")
                }
//                excludedDependencies.forEach {
//                    val s = it.split(":")
//                    conf.exclude(s[0], s[1])
//                }
            }

        }

    }

}
