# Update Versions Gradle Plugin

[![Maven Central](https://img.shields.io/maven-central/v/se.bjurr.gradle.update-versions/se.bjurr.gradle.update-versions.gradle.plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/se.bjurr.gradle.update-versions/se.bjurr.gradle.update-versions.gradle.plugin)

Uses [gradle-versions-plugin](https://github.com/ben-manes/gradle-versions-plugin) to supply tasks that can show updateable dependencies and also update them.

## Usage

Apply it with:

```groovy
plugins {
 id "se.bjurr.gradle.update-versions" version "X"
}
```

It adds these tasks:

- `./gradlew showUpdateableDependencies` - Print list of dependencies that can be updated.
- `./gradlew updateDependencies` - Update dependencies that can be updated.

It will:

- Work with both Groovy `.gradle` and Kotlin `.gradle.kts` files.
- Look for both `dependencies` and `plugins DSL`.

It can be tweaked:

```groovy
updateVersions {
  ignoreDependenciesRegexp.set("anything matching this regexp will be ignored")
}
```

Works great with https://github.com/tomasbjerre/gradle-conventions
