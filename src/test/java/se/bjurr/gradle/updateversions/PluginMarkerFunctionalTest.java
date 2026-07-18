package se.bjurr.gradle.updateversions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Covers the "plugins {}" version-bump rewrite path. The marker artifact is declared a second time
 * as a regular `implementation` dependency so gradle-versions-plugin reports it as outdated - that
 * lets the fixture avoid needing a real, loadable plugin implementation, since every case here uses
 * "apply false" (a real implementation class is only required when a plugin is actually applied).
 */
class PluginMarkerFunctionalTest extends AbstractFunctionalTest {

  private static final String MARKER_DEPENDENCY_GROOVY =
      "implementation \"com.example.fixture.plugin:com.example.fixture.plugin.gradle.plugin:1.0.0\"\n";

  private static final String MARKER_DEPENDENCY_KOTLIN =
      "implementation(\"com.example.fixture.plugin:com.example.fixture.plugin.gradle.plugin:1.0.0\")\n";

  @Test
  void rewritesGroovyPluginsBlockWithApplyFalse() throws IOException {
    writeSettingsGradle();
    writeFile(
        "build.gradle",
        "plugins {\n"
            + "  id \"java-library\"\n"
            + "  id \"se.bjurr.gradle.update-versions\"\n"
            + "  id \"com.example.fixture.plugin\" version \"1.0.0\" apply false\n"
            + "}\n"
            + "\n"
            + "repositories {\n"
            + "  maven { url = uri(\""
            + fixtureRepoUri()
            + "\") }\n"
            + "}\n"
            + "\n"
            + "dependencies {\n"
            + "  "
            + MARKER_DEPENDENCY_GROOVY
            + "}\n");

    runner("updateDependencies").build();

    assertThat(readBuildFile())
        .contains("id \"com.example.fixture.plugin\" version \"2.0.0\" apply false");
  }

  @Test
  void rewritesKotlinPluginsBlockWithApplyFalse() throws IOException {
    writeSettingsGradle();
    writeFile(
        "build.gradle.kts",
        "plugins {\n"
            + "  id(\"java-library\")\n"
            + "  id(\"se.bjurr.gradle.update-versions\")\n"
            + "  id(\"com.example.fixture.plugin\") version \"1.0.0\" apply false\n"
            + "}\n"
            + "\n"
            + "repositories {\n"
            + "  maven { url = uri(\""
            + fixtureRepoUri()
            + "\") }\n"
            + "}\n"
            + "\n"
            + "dependencies {\n"
            + "  "
            + MARKER_DEPENDENCY_KOTLIN
            + "}\n");

    runner("updateDependencies").build();

    assertThat(readBuildFileKts())
        .contains("id(\"com.example.fixture.plugin\") version \"2.0.0\" apply false");
  }
}
