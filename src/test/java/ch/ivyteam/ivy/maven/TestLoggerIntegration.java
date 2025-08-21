package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.compile.CompileProjectMojo;
import ch.ivyteam.ivy.maven.compile.LocalRepoMojoRule;

public class TestLoggerIntegration extends BaseEngineProjectMojoTest {
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

  @Before
  public void setup() {
    System.setOut(new PrintStream(outContent));
  }

  @After
  public void restoreStreams() {
    System.setOut(originalOut);
  }

  @Rule
  public LocalRepoMojoRule<CompileProjectMojo> compile = new LocalRepoMojoRule<>(CompileProjectMojo.GOAL);

  /**
   * regression test for accidentially broken SLF4J dependencies.
   */
  @Test
  public void forwardEngineLogsToMavenConsole() {
    var engineClassLoader = compile.getMojo().getEngineClassloaderFactory();
    var slf4jJars = engineClassLoader.getSlf4jJars();
    assertThat(slf4jJars).hasSize(3);
    assertThat(slf4jJars.get(0).getFileName().toString()).startsWith("slf4j-api-");
    assertThat(outContent.toString())
        .as("no warnings must be printed during slf4j library re-solving from local repo")
        .isEmpty();
  }
}
