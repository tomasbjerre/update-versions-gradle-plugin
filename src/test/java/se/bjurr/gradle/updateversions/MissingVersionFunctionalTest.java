package se.bjurr.gradle.updateversions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class MissingVersionFunctionalTest extends AbstractFunctionalTest {

  @Test
  void setsVersionForStringNotationWithNoVersionAtAll() throws IOException {
    writeGroovyProject(
        "dependencies {\n" + "  implementation \"com.example.fixture:widget\"\n" + "}\n");

    BuildResult result = runner("updateDependencies").build();

    assertThat(readBuildFile()).contains("\"com.example.fixture:widget:2.0.0\"");
    assertThat(result.getOutput()).contains("com.example.fixture:widget -> 2.0.0");
  }

  @Test
  void setsVersionForStringNotationWithEmptyTrailingVersion() throws IOException {
    writeGroovyProject(
        "dependencies {\n" + "  implementation \"com.example.fixture:widget:\"\n" + "}\n");

    runner("updateDependencies").build();

    assertThat(readBuildFile()).contains("\"com.example.fixture:widget:2.0.0\"");
  }

  @Test
  void setsVersionForMapNotationWithoutVersionKey() throws IOException {
    writeGroovyProject(
        "dependencies {\n"
            + "  implementation group: 'com.example.fixture', name: 'widget'\n"
            + "}\n");

    runner("updateDependencies").build();

    assertThat(readBuildFile()).contains("\"com.example.fixture:widget:2.0.0\"");
  }

  @Test
  void doesNotTouchMapNotationThatAlreadyHasAVersion() throws IOException {
    // Regression guard for the negative lookahead in the map-notation "missing version"
    // pattern - it must not misfire on a dependency that already has a version key. Uses the
    // already-latest version so the normal "outdated" rewrite path can't fire either, isolating
    // this to only the missing-version pattern.
    writeGroovyProject(
        "dependencies {\n"
            + "  implementation group: 'com.example.fixture', name: 'widget', version: '2.0.0'\n"
            + "}\n");

    BuildResult result = runner("updateDependencies").build();

    assertThat(readBuildFile())
        .contains("group: 'com.example.fixture', name: 'widget', version: '2.0.0'");
    assertThat(result.getOutput()).contains("All dependencies are up-to-date");
  }

  @Test
  void doesNotSetMissingVersionWhenExplicitlyDisabled() throws IOException {
    writeGroovyProject(
        "updateVersions {\n"
            + "  setMissingVersions.set(false)\n"
            + "}\n\n"
            + "dependencies {\n"
            + "  implementation \"com.example.fixture:widget\"\n"
            + "}\n");

    BuildResult result = runner("updateDependencies").build();

    assertThat(readBuildFile()).contains("\"com.example.fixture:widget\"");
    assertThat(result.getOutput()).contains("All dependencies are up-to-date");
  }

  @Test
  void doesNotSetMissingVersionWhenDowngradeUnresolvedDependenciesIsDisabled() throws IOException {
    // setMissingVersions defaults to true, but filling in a missing version reuses the same
    // resolution mechanism as downgradeUnresolvedDependencies and is gated behind it too.
    writeGroovyProject(
        "updateVersions {\n"
            + "  downgradeUnresolvedDependencies.set(false)\n"
            + "}\n\n"
            + "dependencies {\n"
            + "  implementation \"com.example.fixture:widget\"\n"
            + "}\n");

    BuildResult result = runner("updateDependencies").build();

    assertThat(readBuildFile()).contains("\"com.example.fixture:widget\"");
    assertThat(result.getOutput()).contains("All dependencies are up-to-date");
  }

  @Test
  void ignoreDependenciesRegexpAppliesToMissingVersionDependenciesToo() throws IOException {
    writeGroovyProject(
        "updateVersions {\n"
            + "  ignoreDependenciesRegexp.set(\"com\\\\.example\\\\.fixture:widget:.*\")\n"
            + "}\n\n"
            + "dependencies {\n"
            + "  implementation \"com.example.fixture:widget\"\n"
            + "}\n");

    runner("updateDependencies").build();

    assertThat(readBuildFile()).contains("\"com.example.fixture:widget\"");
  }
}
