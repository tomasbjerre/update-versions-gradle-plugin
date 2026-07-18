package se.bjurr.gradle.updateversions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class IgnoreDependenciesRegexpFunctionalTest extends AbstractFunctionalTest {

  @Test
  void ignoredDependencyIsNotUpdated() throws IOException {
    writeGroovyProject(
        "updateVersions {\n"
            + "  ignoreDependenciesRegexp.set(\"com\\\\.example\\\\.fixture:widget:.*\")\n"
            + "}\n"
            + "\n"
            + "dependencies {\n"
            + "  implementation \"com.example.fixture:widget:1.0.0\"\n"
            + "}\n");

    runner("updateDependencies").build();

    assertThat(readBuildFile()).contains("\"com.example.fixture:widget:1.0.0\"");
  }
}
