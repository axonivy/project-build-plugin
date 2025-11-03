package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.compile.CompileProjectMojo;
import ch.ivyteam.ivy.maven.engine.Slf4jSimpleEngineProperties;
import ch.ivyteam.ivy.maven.extension.LocalRepoTest;
import ch.ivyteam.ivy.maven.extension.SysoutExtension;
import ch.ivyteam.ivy.maven.extension.SysoutExtension.Sysout;

@MojoTest
@ExtendWith(SysoutExtension.class)
public class TestLoggerIntegration {

  private Sysout sysout;

  @BeforeEach
  void setup(Sysout outContent) {
    Slf4jSimpleEngineProperties.install();
    Slf4jSimpleEngineProperties.enforceSimpleConfigReload();
    this.sysout = outContent;
  }

  /**
   * regression test for accidentally broken SLF4J dependencies.
   */
  @Test
  @InjectMojo(goal = CompileProjectMojo.GOAL)
  void forwardEngineLogsToMavenConsole(CompileProjectMojo compile) {
    compile.localRepository = LocalRepoTest.repo();
    var engineClassLoader = compile.getEngineClassloaderFactory();
    var slf4jJars = engineClassLoader.getSlf4jJars();
    assertThat(slf4jJars).hasSize(3);
    assertThat(slf4jJars.get(0).getName().toString()).startsWith("slf4j-api-");
    compile.getLog().info("log setup");
    assertThat(this.sysout.toString())
        .as("no warnings must be printed during slf4j library re-solving from local repo")
        .isEqualToIgnoringNewLines("[INFO] log setup");
  }

}
