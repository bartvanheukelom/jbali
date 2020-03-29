// create file buildSrc/build.gradle.kts in parent project with this content:

plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(file("../PATH_TO_JBALI/buildSrc/src/main/kotlin"))
        }
    }
}
