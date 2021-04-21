import org.jbali.gradle.*
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // supplied by included build `gradle-tools`
        "classpath"("org.jbali:jbali-gradle-tools")
    }
}

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")

    // documentation generation
    // https://github.com/Kotlin/dokka
    id("org.jetbrains.dokka")
}

// check gradle version, for when not running with the included wrapper (e.g. included in another project)
val supportedGradleVersions = setOf(
    "6.7.1",
    "7.0"
)
check(org.gradle.util.GradleVersion.current().version in supportedGradleVersions) {
    "This build script is untested with Gradle version ${org.gradle.util.GradleVersion.current()}. Tested versions are $supportedGradleVersions"
}

initKotlinProject(
    group = JBali.group,
    name = JBali.aJbali,
    acceptableKotlinVersionStrings = setOf("1.5.0-RC")
//        acceptableKotlinVersions = setOf(
//                KotlinVersions.V1_5_0
//        )
)

// TODO centralize
check(kotlinVersionString == KotlinCompilerVersion.VERSION)



repositories {
    mavenCentral()
    kotlinxHtmlJbSpace()
}

kotlin {

    sourceSets {

        commonMain {
            dependencies {
                api(KotlinX.Serialization.json, "1.1.0")
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

tasks.withType<KotlinCompile<*>>().configureEach {
    kotlinOptions {
        inlineClasses()
        use(Experimentals.RequiresOptIn)
    }
}

forbidDependencies(
        // these conflict with the new KotlinX.Serialization.json,
        // but somebody may try to pull them in transitively
        KotlinX.SerializationRuntime.common,
        KotlinX.SerializationRuntime.jvm,
        KotlinX.SerializationRuntime.js
)


// configure `dokkaHtml` to run as part of `build`, but last
tasks {
    val dokkaHtml by existing {
        shouldRunAfter(
                check,
                assemble
        )
    }
    build {
        dependsOn(dokkaHtml)
    }
}


// determine which platforms to include - TODO fix or remove
//val jsOnly = System.getProperty("$name.jsOnly") == "true"
//val doJvm = System.getProperty("$name.doJvm") == "true" || !jsOnly
//val doJs = System.getProperty("$name.doJs") != "false" || jsOnly
val doJvm = true
val doJs = true


// ===================================== JVM ================================ //

if (doJvm) {

    kotlin {

        jvm {

            withJava()

            sourceSets {

                val vKtor = "1.5.3"
                val vSlf4j = "1.7.30"

                val jvmMain by existing {
                    dependencies {
                        api(Kotlin.reflect)

                        api(Arrow.core, "0.10.5")
                        api("org.jetbrains:annotations", "15.0")

                        api("com.google.guava:guava", "29.0-jre")
                        api("org.slf4j:slf4j-api", vSlf4j)

                        api("commons-codec:commons-codec", "1.10")
                        api("org.apache.httpcomponents:httpclient", "4.5.13")
                        api("org.apache.httpcomponents:httpcore", "4.4.13")
                        api("org.threeten:threeten-extra", "1.5.0")

                        // for test library code used in other projects' actual tests
                        compileOnly(Kotlin.Test.jvm)

                        // TODO extract these to own modules or features, see
                        //      https://docs.gradle.org/current/userguide/feature_variants.html
                        compileOnly(Ktor.Client.cio,    vKtor)
                        compileOnly(Ktor.Server.core,   vKtor)
                        compileOnly(Ktor.serialization, vKtor)
                        compileOnly(Ktor.websockets,    vKtor)

                        compileOnly("com.google.code.gson:gson", "2.8.6")
                        compileOnly("org.apache.activemq:activemq-client", "5.11.1")

                    }
                }
                val jvmTest by existing {
                    dependencies {
                        implementation(Kotlin.Test.jvm)
                        implementation(Kotlin.Test.junit)
                        implementation("org.slf4j:jul-to-slf4j",   vSlf4j)
                        implementation("org.slf4j:jcl-over-slf4j", vSlf4j)
                        implementation("org.slf4j:slf4j-simple",   vSlf4j)

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

    tasks.withType<JavaCompile>().configureEach {
        options.storeParameterNames()
    }

    // TODO make KotlinJvmTarget extension
    val javaVersion = JavaVersion.VERSION_1_8
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = sourceCompatibility
    }
    tasks.withType<KotlinJvmCompile>().configureEach {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
        }
    }

    // TODO why is this suddenly required since upgrade to gradle 7.0 / kotlin 1.5.0-RC ?
    val jvmProcessResources by tasks.existing(Copy::class) {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

}





// ===================================== JavaScript ================================ //

if (doJs) {

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

}

