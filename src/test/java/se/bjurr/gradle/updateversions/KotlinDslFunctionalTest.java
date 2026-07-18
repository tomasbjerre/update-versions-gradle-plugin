package se.bjurr.gradle.updateversions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class KotlinDslFunctionalTest extends AbstractFunctionalTest {

  @Test
  void rewritesStringNotationDependencyInKotlinDsl() throws IOException {
    writeKotlinProject(
        "dependencies {\n" + "  implementation(\"com.example.fixture:widget:1.0.0\")\n" + "}\n");

    runner("updateDependencies").build();

    assertThat(readBuildFileKts()).contains("implementation(\"com.example.fixture:widget:2.0.0\")");
  }
}
