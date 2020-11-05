import org.jbali.gradle.*
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

initKotlinProject(
        group = JBali.group,
        name = JBali.aJbali,
//        acceptableKotlinVersions = setOf(
//                KotlinVersions.V1_4_10
//        )
        acceptableKotlinVersionStrings = setOf("1.4.20-RC")
)

// TODO centralize
check(kotlinVersionString == KotlinCompilerVersion.VERSION)

tasks.withType<KotlinCompile> {
    kotlinOptions {
        inlineClasses()
        optIn(Experimentals.RequiresOptIn)
    }
}

libDependencies {

    jcenter()

    val ksr = "1.0.1"
    val slf4j = "1.7.25"
    val ktor = "1.4.1"

    compileAndTest(Kotlin.StdLib.jdk8)
    compileAndTest(Kotlin.reflect)
    compileAndTest(KotlinX.Serialization.json, ksr)

    // TODO https://docs.gradle.org/current/userguide/feature_variants.html
    compileAndTest(Ktor.Server.core, ktor)
    compileAndTest(Ktor.websockets, ktor)
    compileAndTest(Ktor.Client.cio, ktor)
    compileAndTest("io.ktor:ktor-serialization", ktor)

    compileAndTest(Arrow.core, "0.8.1")

    compileAndTest("com.google.guava:guava", "28.0-jre")
    compileAndTest("org.slf4j:slf4j-api", slf4j)
    compileAndTest("com.google.code.gson:gson", "2.3.1")
    compileAndTest("org.apache.activemq:activemq-client", "5.11.1")
    compileAndTest("commons-codec:commons-codec", "1.10")
    compileAndTest("org.apache.httpcomponents:httpclient", "4.4")
    compileAndTest("org.apache.httpcomponents:httpcore", "4.4")
    compileAndTest("org.threeten:threeten-extra", "1.5.0")

    // required to compile shared testing code in main sourceset
    compileAndTest(kotlin("test"))

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.slf4j:jul-to-slf4j", slf4j)
    testImplementation("org.slf4j:jcl-over-slf4j", slf4j)
    testImplementation("org.slf4j:slf4j-simple", slf4j)
    testImplementation("junit:junit", "4.12")

}
