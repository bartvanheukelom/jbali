import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

plugins {
    kotlin("jvm")
    // TODO https://docs.gradle.org/current/userguide/kotlin_dsl.html#sec:kotlin-dsl_plugin
//    `kotlin-dsl`
}

val kotlinVersion = (gradle as ExtensionAware).extra["kotlinVersion"] as KotlinVersion

val kotlinPlugin = plugins.getPlugin(KotlinPluginWrapper::class.java)
val kotlinPluginVersion = kotlinPlugin.kotlinPluginVersion
check(kotlinPluginVersion == "$kotlinVersion") {
    "kotlinPluginVersion $kotlinPluginVersion != kotlinVersion $kotlinVersion"
}

repositories {
    jcenter()
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}

kotlin {
    sourceSets {
        main {
            // enable this if you're using this file as template in outer buildSrc
//            kotlin.srcDir(file("../subs/jbali/buildSrc/src/main/kotlin"))
        }
    }

    dependencies {

        implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
        {
            version {
                strictly("$kotlinVersion")
            }
        }
        // TODO use correct version for gradle version / kotlin version
        // TODO enabling this yields:
        //      org.jetbrains.kotlin.konan.target.HostManager.<init>(Lorg/jetbrains/kotlin/konan/target/SubTargetProvider;ILkotlin/jvm/internal/DefaultConstructorMarker;)V
//        implementation("org.gradle.kotlin:plugins:1.3.5")

    }
}
