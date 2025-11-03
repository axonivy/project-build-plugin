package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import ch.ivyteam.ivy.maven.engine.Slf4jSimpleEngineProperties;
import ch.ivyteam.ivy.maven.extension.SysoutExtension;
import ch.ivyteam.ivy.maven.extension.SysoutExtension.Sysout;

@ExtendWith(SysoutExtension.class)
class TestSlf4jWarningConfiguration {

  @BeforeEach
  void setup() {
    Slf4jSimpleEngineProperties.install();
  }

  /*
   * XIVY-3123 Streamline the log output to be maven-like, instead of logging
   * [WARN] we want [WARNING]. This allows us to use the maven log parser on our
   * jenkins pipelines to avoid introducing new warnings.
   */
  @Test
  void mavenLikeWarning() {
    assertThat(System.getProperty(SimpleLogger.WARN_LEVEL_STRING_KEY))
        .as("SLF4J warning string is not maven-like [WARNING]")
        .isEqualTo("WARNING");
  }

  @Test
  void mavenLoggerWarningOut(Sysout sysout) {
    var logger = LoggerFactory.getLogger("maven.cli");
    logger.warn("hey");

    String out = sysout.toString();
    assertThat(out)
        .as("WARNING bracket matches Maven CLI")
        .startsWith("[WARNING] hey");
  }

}
