import org.jbali.gradle.*
import org.jbali.gradle.git.GitRepository
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

    // adds task `dependencyUpdates` which checks dependencies for available new versions
    // https://github.com/ben-manes/gradle-versions-plugin
    id("com.github.ben-manes.versions") version "0.38.0"
}

// check gradle version, for when not running with the included wrapper (e.g. included in another project)
val recommendedGradleVersion = "8.3"
val supportedGradleVersions = setOf(
    recommendedGradleVersion,
)
check(GradleVersion.current().version in supportedGradleVersions) {
    "This build script is untested with Gradle version ${GradleVersion.current()}. Tested versions are $supportedGradleVersions"
}

initKotlinProject(
    group = JBali.group,
    name = JBali.aJbali,
    acceptableKotlinVersions = setOf(
        KotlinVersions.V1_9_10,
    )
)

// TODO centralize
check(kotlinVersionString == KotlinCompilerVersion.VERSION)
val kLibVs = kotlinLinkedLibVersions.getValue(kotlinVersion!!)

val javaVersionMajor = projectOrBuildProp("java.version.major").toString().toInt()
val javaVersion = JavaVersion.toVersion(javaVersionMajor)
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersionMajor))
    }
}

repositories {
    mavenCentral()
    kotlinxHtmlJbSpace()
}

kotlin {

    sourceSets {

        commonMain {
            dependencies {
                api(KotlinX.Serialization.json, kLibVs.serialization)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

    }
}

tasks {
    
    val wrapper by existing(Wrapper::class) {
        gradleVersion = recommendedGradleVersion
        distributionType = Wrapper.DistributionType.ALL  // get sources
        distributionSha256Sum = "47a5bfed9ef814f90f8debcbbb315e8e7c654109acd224595ea39fca95c5d4da"
    }
    
    withType<KotlinCompile<*>>().configureEach {
        kotlinOptions {
            setBackendThreads()
            compilerXArg("context-receivers")
        }
    }
}

forbidDependencies(
    // these conflict with the new KotlinX.Serialization.json,
    // but somebody may try to pull them in transitively
    KotlinX.SerializationRuntime.common,
    KotlinX.SerializationRuntime.jvm,
    KotlinX.SerializationRuntime.js
)


tasks {

    val gitVersion by registering {
        doLast {
            println(GitRepository(projectDir).version())
        }
    }


    // configure `dokkaHtml` to run as part of `build`, but last
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

                val vKtor = "1.6.5"
                val vOTel = "1.46.0"
                val vSlf4j = "1.7.32"
                val vExposed = "0.35.3"
                
                val jvmMain by existing {
                    dependencies {
                        api(Kotlin.reflect)
                        
                        api(Arrow.core, "1.0.1")
                        api("org.jetbrains:annotations", "23.0.0")
                        api("javax.annotation:javax.annotation-api", "1.3.2")
                        api("javax.xml.bind:jaxb-api", "2.3.0")
                        
                        api("com.google.guava:guava", "31.1-jre")
                        api("org.slf4j:slf4j-api", vSlf4j)
                        api("io.micrometer:micrometer-core:1.11.0")
                        
                        compileOnly("io.opentelemetry:opentelemetry-sdk", vOTel)
                        compileOnly("io.opentelemetry:opentelemetry-sdk-trace", vOTel)
                        compileOnly("io.opentelemetry:opentelemetry-context", vOTel)
                        compileOnly("io.opentelemetry:opentelemetry-extension-kotlin",
                            // vOTel  // pulls in kotlin 2.x - TODO instead of weird compile errors, make gradle fail the resolution
                            "1.41.0"
                        )
                        
                        api("commons-codec:commons-codec", "1.15")
                        api("org.apache.commons:commons-lang3", "3.12.0")
                        api("org.apache.httpcomponents:httpclient", "4.5.13")
                        api("org.apache.httpcomponents:httpcore", "4.4.14")
                        api("org.threeten:threeten-extra", "1.7.1")
                        
                        // for test library code used in other projects' actual tests
                        compileOnly(Kotlin.Test.jvm)
                        
                        // TODO extract these to own modules or features, see
                        //      https://docs.gradle.org/current/userguide/feature_variants.html
                        compileOnly(Ktor.Client.cio,          vKtor)
                        compileOnly(Ktor.Client.Logging.jvm,  vKtor)
                        compileOnly("io.ktor:ktor-client-auth", vKtor)
                        compileOnly(Ktor.Server.core,         vKtor)
                        compileOnly(Ktor.serialization,       vKtor)
                        compileOnly(Ktor.websockets,          vKtor)
                        
                        compileOnly("org.jetbrains.exposed:exposed-core",      vExposed)
                        compileOnly("org.jetbrains.exposed:exposed-dao",       vExposed)
                        compileOnly("org.jetbrains.exposed:exposed-java-time", vExposed)
                        compileOnly("org.jetbrains.exposed:exposed-jdbc",      vExposed)
                        
                        compileOnly("com.google.code.gson:gson", "2.10")
                        compileOnly("org.springframework:spring-context", "5.3.23")
                        
                    }
                }
                val jvmTest by existing {
                    dependencies {
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
            
            // configure jvmTest task
            tasks {
                withType<Test>().configureEach {
//                    println("config $this")
                    jvmArgs(
                        "-Dorg.slf4j.simpleLogger.showDateTime=true", // TODO doesn't work, but the println says :jvmTest is configured
                        // for JavaSerializer.assertReadResolve etc.
                        "--add-opens", "java.base/java.io=ALL-UNNAMED",
                        // print GC info
                        "-verbose:gc",
                    )
                }
            }
            
        }
    }
    
    tasks.withType<JavaCompile>().configureEach {
        options.storeParameterNames()
//        options.compilerArgs.add("-Dfile.encoding=UTF-8")
    }
    
    tasks.withType<KotlinJvmCompile>().configureEach {
        kotlinOptions {
            setJvmTarget(javaVersion)
            jvmDefaultAll()
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
        
    }
    
}
