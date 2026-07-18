package se.bjurr.gradle.updateversions;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.io.TempDir;

abstract class AbstractFunctionalTest {

  @TempDir Path projectDir;

  protected static String fixtureRepoUri() {
    URL url = AbstractFunctionalTest.class.getClassLoader().getResource("test-repo");
    if (url == null) {
      throw new IllegalStateException("test-repo fixture not found on test classpath");
    }
    try {
      return Path.of(url.toURI()).toUri().toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  protected void writeSettingsGradle() throws IOException {
    Files.writeString(
        projectDir.resolve("settings.gradle"),
        "pluginManagement {\n"
            + "  repositories {\n"
            + "    gradlePluginPortal()\n"
            + "    mavenCentral()\n"
            + "    maven { url = uri(\""
            + fixtureRepoUri()
            + "\") }\n"
            + "  }\n"
            + "}\n");
  }

  protected void writeFile(String fileName, String content) throws IOException {
    Files.writeString(projectDir.resolve(fileName), content);
  }

  /**
   * Writes a Groovy DSL test project applying the plugin under test, with the given content
   * appended to build.gradle.
   */
  protected void writeGroovyProject(String extraBuildGradle) throws IOException {
    writeSettingsGradle();
    Files.writeString(
        projectDir.resolve("build.gradle"),
        "plugins {\n"
            + "  id \"java-library\"\n"
            + "  id \"se.bjurr.gradle.update-versions\"\n"
            + "}\n"
            + "\n"
            + "repositories {\n"
            + "  maven { url = uri(\""
            + fixtureRepoUri()
            + "\") }\n"
            + "}\n"
            + "\n"
            + extraBuildGradle);
  }

  /**
   * Writes a Kotlin DSL test project applying the plugin under test, with the given content
   * appended to build.gradle.kts.
   */
  protected void writeKotlinProject(String extraBuildGradle) throws IOException {
    writeSettingsGradle();
    Files.writeString(
        projectDir.resolve("build.gradle.kts"),
        "plugins {\n"
            + "  id(\"java-library\")\n"
            + "  id(\"se.bjurr.gradle.update-versions\")\n"
            + "}\n"
            + "\n"
            + "repositories {\n"
            + "  maven { url = uri(\""
            + fixtureRepoUri()
            + "\") }\n"
            + "}\n"
            + "\n"
            + extraBuildGradle);
  }

  protected String readBuildFile() throws IOException {
    return Files.readString(projectDir.resolve("build.gradle"));
  }

  protected String readBuildFileKts() throws IOException {
    return Files.readString(projectDir.resolve("build.gradle.kts"));
  }

  protected GradleRunner runner(String... args) {
    return GradleRunner.create()
        .withPluginClasspath()
        .withProjectDir(projectDir.toFile())
        .withArguments(args)
        .forwardOutput();
  }
}
