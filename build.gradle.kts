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

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    val slf4j = "1.7.25"

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-runtime")

    implementation("io.arrow-kt:arrow-core:0.8.1")

    implementation("com.google.guava:guava:28.0-jre")
    implementation("org.slf4j:slf4j-api:$slf4j")
    implementation("com.google.code.gson:gson:2.3.1")
    implementation("org.apache.activemq:activemq-client:5.11.1")
    implementation("commons-codec:commons-codec:1.10")
    implementation("org.apache.httpcomponents:httpclient:4.4")
    implementation("org.apache.httpcomponents:httpcore:4.4")

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
