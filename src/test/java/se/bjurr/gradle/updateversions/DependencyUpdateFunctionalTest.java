package se.bjurr.gradle.updateversions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class DependencyUpdateFunctionalTest extends AbstractFunctionalTest {

  @Test
  void rewritesStringNotationDependency() throws IOException {
    writeGroovyProject(
        "dependencies {\n"
            + "  implementation \"com.example.fixture:widget:1.0.0\"\n"
            + "}\n"
            + "\n"
            + "// This comment should remain untouched\n");

    BuildResult result = runner("updateDependencies").build();

    assertThat(readBuildFile()).contains("\"com.example.fixture:widget:2.0.0\"");
    assertThat(readBuildFile()).contains("// This comment should remain untouched");
    assertThat(result.getOutput()).contains("com.example.fixture:widget:1.0.0 -> 2.0.0");
  }

  @Test
  void rewritesMapNotationDependencyToStringNotation() throws IOException {
    // The plugin normalizes map-notation dependencies to string notation when it rewrites them.
    writeGroovyProject(
        "dependencies {\n"
            + "  implementation group: 'com.example.fixture', name: 'widget', version: '1.0.0'\n"
            + "}\n");

    runner("updateDependencies").build();

    String content = readBuildFile();
    assertThat(content).contains("\"com.example.fixture:widget:2.0.0\"");
    assertThat(content).doesNotContain("group:");
  }
}
