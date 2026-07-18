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

Example:

```sh
$ ./gradlew uD

> Task :updateDependencies

📝 Updated dependencies:

  ➕ se.bjurr.gitchangelog:git-changelog-lib -> 2.6.3
  ➕ se.bjurr.violations:violations-git-lib -> 2.5.5
  ➕ se.bjurr.violations:violations-lib -> 1.160.5
  ⬇️ org.immutables:value:5.11.2 -> 2.12.2
  📦 org.immutables:value:2.11.2 -> 2.12.2
  📦 plugin se.bjurr.gitchangelog.git-changelog-gradle-plugin:3.0.0 -> 3.1.2
  📦 plugin se.bjurr.gradle.update-versions:latest-SNAPSHOT -> 1.6.1
```

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
  /*
   * If a dependency is declared with no version at all - 'group:name',
   * 'group:name:' or the map notation without a version key - set it
   * to the latest resolvable version. Reuses the same resolution as
   * downgradeUnresolvedDependencies, so it only has an effect while
   * that is also enabled. On by default.
   */
  setMissingVersions.set(false)
}
```

Works great with https://github.com/tomasbjerre/gradle-conventions
