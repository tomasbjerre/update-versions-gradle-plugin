# Update Versions Gradle Plugin

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/se.bjurr.gradle.update-versions)](https://plugins.gradle.org/plugin/se.bjurr.gradle.update-versions)

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
