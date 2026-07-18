package se.bjurr.gradle.updateversions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class DowngradeUnresolvedDependencyFunctionalTest extends AbstractFunctionalTest {

  // downgradeUnresolvedDependencies defaults to true, so most tests don't need to set it -
  // this constant is only used where a test wants to be explicit about it being enabled.
  private static final String DOWNGRADE_ENABLED =
      "updateVersions {\n" + "  downgradeUnresolvedDependencies.set(true)\n" + "}\n\n";

  private static final String DOWNGRADE_DISABLED =
      "updateVersions {\n" + "  downgradeUnresolvedDependencies.set(false)\n" + "}\n\n";

  @Test
  void updateDependenciesDowngradesToHighestResolvableVersionBelowCurrent() throws IOException {
    // "9.9.9" does not exist for "broken" at all - lands in gradle-versions-plugin's
    // "unresolved" bucket. The highest actually-resolvable version below it is 1.5.0.
    writeGroovyProject(
        DOWNGRADE_ENABLED
            + "dependencies {\n"
            + "  implementation \"com.example.fixture:broken:9.9.9\"\n"
            + "}\n");

    BuildResult result = runner("updateDependencies").build();

    assertThat(readBuildFile()).contains("\"com.example.fixture:broken:1.5.0\"");
    assertThat(result.getOutput()).contains("com.example.fixture:broken:9.9.9 -> 1.5.0");
  }

  @Test
  void showUpdateableDependenciesReportsDowngradeWithoutModifyingTheFile() throws IOException {
    String original =
        DOWNGRADE_ENABLED
            + "dependencies {\n"
            + "  implementation \"com.example.fixture:broken:9.9.9\"\n"
            + "}\n";
    writeGroovyProject(original);

    BuildResult result = runner("showUpdateableDependencies").build();

    assertThat(readBuildFile()).endsWith(original.substring(DOWNGRADE_ENABLED.length()));
    assertThat(result.getOutput()).contains("com.example.fixture:broken:9.9.9 -> 1.5.0");
  }

  @Test
  void doesNotDowngradeWhenTheFallbackVersionIsAlsoUnresolvable() throws IOException {
    // "2.0.0" does not exist for "hopeless" - lands in "unresolved". The only version below
    // it, 1.0.0, has a POM but no jar, so the single fallback attempt also fails to resolve.
    writeGroovyProject(
        DOWNGRADE_ENABLED
            + "dependencies {\n"
            + "  implementation \"com.example.fixture:hopeless:2.0.0\"\n"
            + "}\n");

    BuildResult result = runner("updateDependencies").build();

    assertThat(readBuildFile()).contains("\"com.example.fixture:hopeless:2.0.0\"");
    assertThat(result.getOutput()).contains("All dependencies are up-to-date");
  }

  @Test
  void downgradesByDefaultWithoutAnyConfiguration() throws IOException {
    // No updateVersions {} block at all - downgradeUnresolvedDependencies defaults to true.
    writeGroovyProject(
        "dependencies {\n" + "  implementation \"com.example.fixture:broken:9.9.9\"\n" + "}\n");

    BuildResult result = runner("updateDependencies").build();

    assertThat(readBuildFile()).contains("\"com.example.fixture:broken:1.5.0\"");
    assertThat(result.getOutput()).contains("com.example.fixture:broken:9.9.9 -> 1.5.0");
  }

  @Test
  void doesNotDowngradeWhenExplicitlyOptedOut() throws IOException {
    writeGroovyProject(
        DOWNGRADE_DISABLED
            + "dependencies {\n"
            + "  implementation \"com.example.fixture:broken:9.9.9\"\n"
            + "}\n");

    BuildResult result = runner("updateDependencies").build();

    assertThat(readBuildFile()).contains("\"com.example.fixture:broken:9.9.9\"");
    assertThat(result.getOutput()).contains("All dependencies are up-to-date");
  }

  @Test
  void combinesUpgradesAndDowngradesInOneRun() throws IOException {
    writeGroovyProject(
        DOWNGRADE_ENABLED
            + "dependencies {\n"
            + "  implementation \"com.example.fixture:widget:1.0.0\"\n"
            + "  implementation \"com.example.fixture:broken:9.9.9\"\n"
            + "}\n");

    runner("updateDependencies").build();

    String content = readBuildFile();
    assertThat(content).contains("\"com.example.fixture:widget:2.0.0\"");
    assertThat(content).contains("\"com.example.fixture:broken:1.5.0\"");
  }
}
