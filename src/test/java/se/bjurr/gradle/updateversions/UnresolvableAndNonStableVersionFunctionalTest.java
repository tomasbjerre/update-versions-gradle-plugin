package se.bjurr.gradle.updateversions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the two bug classes called out in the CHANGELOG: "avoid updating
 * dependencies that cannot be fetched" and "avoid update to quarantined dependencies".
 */
class UnresolvableAndNonStableVersionFunctionalTest extends AbstractFunctionalTest {

  @Test
  void doesNotUpdateToAVersionThatCannotBeResolved() throws IOException {
    // "flaky" advertises 2.0.0 in its metadata, but no jar is published for it.
    writeGroovyProject(
        "dependencies {\n" + "  implementation \"com.example.fixture:flaky:1.0.0\"\n" + "}\n");

    BuildResult result = runner("updateDependencies").build();

    assertThat(readBuildFile()).contains("\"com.example.fixture:flaky:1.0.0\"");
    assertThat(result.getOutput()).contains("All dependencies are up-to-date");
  }

  @Test
  void doesNotUpdateToANonStableVersion() throws IOException {
    // "prerelease" only has a newer "2.0.0-alpha" version, which isNonStable() rejects.
    writeGroovyProject(
        "dependencies {\n" + "  implementation \"com.example.fixture:prerelease:1.0.0\"\n" + "}\n");

    BuildResult result = runner("updateDependencies").build();

    assertThat(readBuildFile()).contains("\"com.example.fixture:prerelease:1.0.0\"");
    assertThat(result.getOutput()).contains("All dependencies are up-to-date");
  }
}
