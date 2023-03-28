jbali
=====

[![](https://travis-ci.org/bartvanheukelom/jbali.svg?branch=master)](https://travis-ci.org/bartvanheukelom/jbali)

Yet another collection of Java, and now Kotlin (JVM/JS), utility classes.

## Upgrading Gradle

- Upgrade `gradle-tools` first, see its README
- Update `recommendedGradleVersion` and `distributionSha256Sum` in `build.gradle.kts`
- Update `settings-tools` Jar path in `settings.gradle.kts`
- Run `cp gradle-tools/gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.properties`
- Run `./gradlew wrapper`
- Run `./gradlew build`
