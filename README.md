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
  /*
   * Dependencies matching `group:name:version` against this regexp 
   * are left alone. Empty by default, so nothing is ignored.
   */
  ignoreDependenciesRegexp.set("regexp")
  /*
   * If a dependency's declared version cannot be resolved (for
   * example a release was retracted or quarantined), fall back to
   * the highest version below it that *can* be resolved, instead of
   * leaving it broken. On by default.
   */
  downgradeUnresolvedDependencies.set(false)
}
```

Works great with https://github.com/tomasbjerre/gradle-conventions
