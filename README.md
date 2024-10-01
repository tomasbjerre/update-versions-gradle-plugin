# Update Versions Gradle Plugin

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.bjurr.gradle.update-versions/se.bjurr.gradle.update-versions.gradle.plugin/badge.svg)](https://search.maven.org/artifact/se.bjurr.gradle.update-versions/se.bjurr.gradle.update-versions.gradle.plugin)

Uses [gradle-versions-plugin](https://github.com/ben-manes/gradle-versions-plugin) to supply tasks that can show updateable dependencies and also update them.

- `./gradlew showUpdateableDependencies` - Print list of dependencies that can be updated.
- `./gradlew updateDependencies` - Update dependencies that can be updated.

It can be tweaked in `gradle.properties` with some properties, the plugin reads them like this:

```groovy
// ---- default config ----
// ignoreDependenciesRegexp: (jakarta.inject|jakarta.servlet).*
ignoreDependenciesRegexp: project.getProperties().getOrDefault("ignoreDependenciesRegexp", ""),
// ---- default config ----
```
