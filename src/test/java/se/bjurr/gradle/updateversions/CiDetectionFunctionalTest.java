package se.bjurr.gradle.updateversions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class CiDetectionFunctionalTest extends AbstractFunctionalTest {

  private static final String[] CI_ENV_VARS = {
    "CI",
    "GITHUB_ACTIONS",
    "GITLAB_CI",
    "TRAVIS",
    "CIRCLECI",
    "JENKINS_URL",
    "TF_BUILD",
    "BUILDKITE"
  };

  private static Map<String, String> environmentWithoutCiMarkers() {
    Map<String, String> env = new HashMap<>(System.getenv());
    for (String var : CI_ENV_VARS) {
      env.remove(var);
    }
    return env;
  }

  @Test
  void buildFinalizesIntoShowUpdateableDependenciesOutsideCi() throws IOException {
    writeGroovyProject("");

    BuildResult result = runner("build").withEnvironment(environmentWithoutCiMarkers()).build();

    assertThat(result.task(":showUpdateableDependencies")).isNotNull();
  }

  @Test
  void buildSkipsShowUpdateableDependenciesInCi() throws IOException {
    writeGroovyProject("");

    Map<String, String> env = environmentWithoutCiMarkers();
    env.put("GITHUB_ACTIONS", "true");
    BuildResult result = runner("build").withEnvironment(env).build();

    assertThat(result.task(":showUpdateableDependencies")).isNull();
    assertThat(result.getOutput())
        .contains("Running in CI, saving time by not showing updatable dependencies");
  }
}
