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
val supportedGradleVersions = setOf(
    "6.7.1",
    "7.0",
    "7.2",
    "7.3"
)
check(org.gradle.util.GradleVersion.current().version in supportedGradleVersions) {
    "This build script is untested with Gradle version ${org.gradle.util.GradleVersion.current()}. Tested versions are $supportedGradleVersions"
}

initKotlinProject(
    group = JBali.group,
    name = JBali.aJbali,
    acceptableKotlinVersions = setOf(
        KotlinVersions.V1_6_0,
    )
)

// TODO centralize
check(kotlinVersionString == KotlinCompilerVersion.VERSION)

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
                api(KotlinX.Serialization.json, "1.3.1")
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
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
                val vSlf4j = "1.7.32"
                val vExposed = "0.35.3"
                
                val jvmMain by existing {
                    dependencies {
                        api(Kotlin.reflect)
                        
                        api(Arrow.core, "1.0.1")
                        api("org.jetbrains:annotations", "20.1.0")
                        api("javax.annotation:javax.annotation-api", "1.3.2")
                        api("javax.xml.bind:jaxb-api", "2.3.0")
                        
                        api("com.google.guava:guava", "31.0.1-jre")
                        api("org.slf4j:slf4j-api", vSlf4j)
                        
                        api("commons-codec:commons-codec", "1.15")
                        api("org.apache.commons:commons-lang3", "3.12.0")
                        api("org.apache.httpcomponents:httpclient", "4.5.13")
                        api("org.apache.httpcomponents:httpcore", "4.4.14")
                        api("org.threeten:threeten-extra", "1.6.0")
                        
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
                        
                        compileOnly("com.google.code.gson:gson", "2.8.6")
                        compileOnly("org.apache.activemq:activemq-client", "5.16.2")
                        compileOnly("org.springframework:spring-context", "5.3.7")
                        
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
