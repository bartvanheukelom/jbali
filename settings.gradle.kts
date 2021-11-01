import org.jbali.gradle.settings.*

buildscript {
    dependencies {
        // unfortunately can't access GradleVersion.current() here,
        // but the included jar will check for version mismatch
        classpath(files("./gradle-tools/settings-tools/lib/gradle-7.2.jar"))
    }
}

rootProject.name = "jbali"
composableBuild()

managePlugins {
    recommendedRepositories()
    versionsFromProperties()
}

compositeBuild {
    includeNamedBuild("jbali-gradle-tools")
}
