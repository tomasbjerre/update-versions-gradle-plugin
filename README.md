# Conventional Release Gradle Pluigin

Bundle of plugins and some Gradle DSL that can publish:

- JAR:s to Maven Central
- Command line tools to Maven Central and NPM
- Gradle plugins to Maven Central and Gradle Plugin Portal

```groovy
plugins {
    id 'se.bjurr.gradle.conventional-release-gradle-plugin' version 'X'
}
```

https://plugins.gradle.org/plugin/se.bjurr.gradle.conventional-release-gradle-plugin

Run it with:

```groovy
./gradlew release
```

It can be tweaked in `gradle.properties` with some properties:

<!-- start default config -->
```groovy

def givenConfig = [
  // repoType: JAR # JAR, GRADLE, COMMAND
  repoType: gradleProps.getProperty("repoType", "JAR"),
  // relocate: org:org,com:com # Empty by default
  relocate: gradleProps.getProperty("relocate", ""),
  website: gradleProps.getProperty("website", "'https://github.com/tomasbjerre/' + project.name + "),
  vcsUrl: gradleProps.getProperty("vcsUrl", "'https://github.com/tomasbjerre/'+project.name + "),
  licenseName: gradleProps.getProperty("licenseName", "'The Apache Software License, Version 2.0'"),
  licenseUrl: gradleProps.getProperty("licenseUrl", "'http://www.apache.org/licenses/LICENSE-2.0.txt'"),
  developerId: gradleProps.getProperty("developerId", "'tomasbjerre'"),
  developerName: gradleProps.getProperty("developerName", "'Tomas Bjerre'"),
  developerEmail: gradleProps.getProperty("developerEmail", "'tomas.bjerre85@gmail.com'"),
  mavenRepositoryName: gradleProps.getProperty("mavenRepositoryName", "'nexus'"),
  mavenRepositoryUrl: gradleProps.getProperty("mavenRepositoryUrl", "'https://oss.sonatype.org/service/local/staging/deploy/maven2/'"),
  nexusCloseAndRelease: gradleProps.getProperty("relnexusCloseAndReleaseocate", "true"),
  sign: gradleProps.getProperty("sign", "true") == "true",
  // tags: a,b,c # Empty by default
  tags: gradleProps.getProperty("tags", ""),
  implementationClass: gradleProps.getProperty("implementationClass", ""),
]

```
<!-- end default config -->
