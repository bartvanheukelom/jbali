import org.jbali.gradle.*

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

libdependencies {

    jcenter()

    val ksr = "0.11.0"
    val slf4j = "1.7.25"

    compileAndTest(kotlin("stdlib-jdk8"))
    compileAndTest(kotlin("reflect"))
    compileAndTest("org.jetbrains.kotlinx:kotlinx-serialization-runtime", ksr)

    compileAndTest("io.arrow-kt:arrow-core", "0.8.1")

    compileAndTest("com.google.guava:guava", "28.0-jre")
    compileAndTest("org.slf4j:slf4j-api", slf4j)
    compileAndTest("com.google.code.gson:gson", "2.3.1")
    compileAndTest("org.apache.activemq:activemq-client", "5.11.1")
    compileAndTest("commons-codec:commons-codec", "1.10")
    compileAndTest("org.apache.httpcomponents:httpclient", "4.4")
    compileAndTest("org.apache.httpcomponents:httpcore", "4.4")

    // required to compile shared testing code in main sourceset
    compileAndTest(kotlin("test"))

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.slf4j:jul-to-slf4j", slf4j)
    testImplementation("org.slf4j:jcl-over-slf4j", slf4j)
    testImplementation("org.slf4j:slf4j-simple", slf4j)
    testImplementation("junit:junit", "4.12")

}
