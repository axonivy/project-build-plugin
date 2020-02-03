package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.slf4j.impl.SimpleLogger;

import ch.ivyteam.ivy.maven.engine.Slf4jSimpleEngineProperties;

public class TestSlf4JConfiguration
{
  @Test
  public void systemPropertiesSet()
  {
    Slf4jSimpleEngineProperties.install();
    assertThat(System.getProperty(SimpleLogger.SHOW_THREAD_NAME_KEY)).isEqualTo("false");
    assertThat(System.getProperty(SimpleLogger.LEVEL_IN_BRACKETS_KEY)).isEqualTo("true");
    assertThat(System.getProperty(SimpleLogger.SHOW_LOG_NAME_KEY)).isEqualTo("false");
    assertThat(System.getProperty(SimpleLogger.WARN_LEVEL_STRING_KEY)).isEqualTo("WARNING");
    assertThat(System.getProperty(SimpleLogger.LOG_KEY_PREFIX+"ch.ivyteam.ivy")).isEqualTo("error");
    assertThat(System.getProperty(SimpleLogger.LOG_KEY_PREFIX+"org.apache.cxf.bus.spring")).isEqualTo("error");
    assertThat(System.getProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY)).isEqualTo("warn");
  }
}
