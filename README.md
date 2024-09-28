# Update Versions Gradle Pluigin

Uses [gradle-versions-plugin](https://github.com/ben-manes/gradle-versions-plugin) to supply tasks that can show updateable dependencies and also update them.

- `./gradlew showUpdateableDependencies` - Print list of dependencies that can be updated.
- `./gradlew updateDependencies` - Update dependencies that can be updated.

```groovy
plugins {
    id 'se.bjurr.gradle.update-versions' version 'X'
}
```

<https://plugins.gradle.org/plugin/se.bjurr.gradle.update-versions>

It can be tweaked in `gradle.properties` with some properties:

<!-- start default config -->
```groovy

def givenConfig = [
  // ignoreDependenciesRegexp: (jakarta\.inject|jakarta\.servlet).*
  ignoreDependenciesRegexp: gradleProps.getProperty("ignoreDependenciesRegexp", ""),
]

```
<!-- end default config -->
