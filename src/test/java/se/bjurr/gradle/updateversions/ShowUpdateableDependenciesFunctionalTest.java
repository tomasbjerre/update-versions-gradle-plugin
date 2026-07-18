package se.bjurr.gradle.updateversions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class ShowUpdateableDependenciesFunctionalTest extends AbstractFunctionalTest {

  @Test
  void reportsUpdatableDependencyWithoutModifyingTheFile() throws IOException {
    String original =
        "dependencies {\n" + "  implementation \"com.example.fixture:widget:1.0.0\"\n" + "}\n";
    writeGroovyProject(original);

    BuildResult result = runner("showUpdateableDependencies").build();

    assertThat(readBuildFile()).endsWith(original);
    assertThat(result.getOutput()).contains("com.example.fixture:widget:1.0.0 -> 2.0.0");
    assertThat(result.getOutput()).contains("updateDependencies");
  }

  @Test
  void reportsAllUpToDateWhenNothingCanBeUpdated() throws IOException {
    writeGroovyProject(
        "dependencies {\n" + "  implementation \"com.example.fixture:widget:2.0.0\"\n" + "}\n");

    BuildResult result = runner("showUpdateableDependencies").build();

    assertThat(result.getOutput()).contains("All dependencies are up-to-date");
    assertThat(readBuildFile()).contains("com.example.fixture:widget:2.0.0");
  }
}
