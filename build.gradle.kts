import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    java
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

repositories {
    jcenter()
}

// TODO make configurable
val jdkVersion = foolCompilerNotConstant(8)

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = if (jdkVersion < 9) "1.$jdkVersion" else "$jdkVersion"
    }
}

dependencies {
    val slf4j = "1.7.25"

    fun compileAndTest(dep: Any) {
        compileOnly(dep)
        testImplementation(dep)
    }

    compileAndTest(kotlin("stdlib-jdk8"))
    compileAndTest(kotlin("reflect"))
    compileAndTest("org.jetbrains.kotlinx:kotlinx-serialization-runtime")

    compileAndTest("io.arrow-kt:arrow-core:0.8.1")

    compileAndTest("com.google.guava:guava:28.0-jre")
    compileAndTest("org.slf4j:slf4j-api:$slf4j")
    compileAndTest("com.google.code.gson:gson:2.3.1")
    compileAndTest("org.apache.activemq:activemq-client:5.11.1")
    compileAndTest("commons-codec:commons-codec:1.10")
    compileAndTest("org.apache.httpcomponents:httpclient:4.4")
    compileAndTest("org.apache.httpcomponents:httpcore:4.4")

    // required to compile shared testing code in main sourceset
    compileAndTest(kotlin("test"))

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.slf4j:jul-to-slf4j:$slf4j")
    testImplementation("org.slf4j:jcl-over-slf4j:$slf4j")
    testImplementation("org.slf4j:slf4j-simple:$slf4j")
    testImplementation("junit:junit:4.12")

    if (project == rootProject) {
        constraints {
            implementation("org.jetbrains.kotlinx", "kotlinx-serialization-runtime", "0.13.0")
        }
    }
}

fun <T> foolCompilerNotConstant(v: T) = v
