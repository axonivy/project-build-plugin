package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.maven.compile.CompileProjectMojo;
import ch.ivyteam.ivy.maven.engine.Slf4jSimpleEngineProperties;
import ch.ivyteam.ivy.maven.extension.LocalRepoTest;
import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.maven.log.LogCollector.LogEntry;

@MojoTest
public class TestLoggerIntegration {

  @BeforeEach
  void setup() {
    Slf4jSimpleEngineProperties.install();
    Slf4jSimpleEngineProperties.enforceSimpleConfigReload();
  }

  /**
   * regression test for accidentally broken SLF4J dependencies.
   */
  @Test
  @InjectMojo(goal = CompileProjectMojo.GOAL)
  void forwardEngineLogsToMavenConsole(CompileProjectMojo compile) {
    var logs = new LogCollector();
    compile.setLog(logs);
    compile.localRepository = LocalRepoTest.repo();
    var engineClassLoader = compile.getEngineClassloaderFactory();
    var slf4jJars = engineClassLoader.getSlf4jJars();
    assertThat(slf4jJars).hasSize(3);
    assertThat(slf4jJars.get(0).getFileName().toString()).startsWith("slf4j-api-");
    compile.getLog().info("log setup");
    assertThat(logs.getLogs()).extracting(LogEntry::toString)
        .as("no warnings must be printed during slf4j library re-solving from local repo")
        .containsOnly("log setup");
  }

}
