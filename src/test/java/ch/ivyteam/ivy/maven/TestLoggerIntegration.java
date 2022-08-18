package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.compile.CompileMojoRule;
import ch.ivyteam.ivy.maven.compile.CompileProjectMojo;
import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory;

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
  public CompileMojoRule<CompileProjectMojo> compile = new CompileMojoRule<>(CompileProjectMojo.GOAL);

  /**
   * regression test for accidentially broken SLF4J dependencies.
   */
  @Test
  public void forwardEngineLogsToMavenConsole() {
    EngineClassLoaderFactory engineClassLoader = compile.getMojo().getEngineClassloaderFactory();
    List<File> slf4jJars = engineClassLoader.getSlf4jJars();
    assertThat(slf4jJars).hasSize(3);
    assertThat(slf4jJars.get(0).getName()).startsWith("slf4j-api-");
    assertThat(outContent.toString())
            .as("no warnings must be printed during slf4j library re-solving from local repo")
            .isEmpty();
  }

}
