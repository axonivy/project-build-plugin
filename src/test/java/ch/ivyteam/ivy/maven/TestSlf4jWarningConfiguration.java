package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.simple.SimpleLogger;

import ch.ivyteam.ivy.maven.engine.Slf4jSimpleEngineProperties;

class TestSlf4jWarningConfiguration {
  /*
   * XIVY-3123 Streamline the log output to be maven-like, instead of logging
   * [WARN] we want [WARNING]. This allows us to use the maven log parser on our
   * jenkins pipelines to avoid introducing new warnings.
   */
  @Test
  void mavenLikeWarning() {
    Slf4jSimpleEngineProperties.install();
    assertThat(System.getProperty(SimpleLogger.WARN_LEVEL_STRING_KEY))
        .as("SLF4J warning string is not maven-like [WARNING]")
        .isEqualTo("WARNING");
  }
}
