package ch.ivyteam.ivy.maven.generate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.compile.LocalRepoMojoRule;
import ch.ivyteam.ivy.maven.util.PathUtils;

public class TestGenerateProjectSourcesMojo {

  @Rule
  public LocalRepoMojoRule<GenerateProjectSourcesMojo> generate = new LocalRepoMojoRule<>(GenerateProjectSourcesMojo.GOAL);

  @Test
  public void generateSources() throws Exception {
    var projectDir = generate.project.getBasedir().toPath();
    var dataClassDir = projectDir.resolve("src_dataClasses");
    var wsProcDir = projectDir.resolve("src_wsproc");
    var classDir = projectDir.resolve("classes");
    var targetClasses = projectDir.resolve("target").resolve("classes");
    PathUtils.delete(dataClassDir);
    PathUtils.delete(wsProcDir);
    PathUtils.delete(classDir);
    PathUtils.delete(targetClasses);

    assertThat(dataClassDir).doesNotExist();
    assertThat(wsProcDir).doesNotExist();
    assertThat(classDir).doesNotExist();
    assertThat(targetClasses).doesNotExist();

    generate.getMojo().execute();

    assertThat(dataClassDir)
        .isDirectoryRecursivelyContaining(f -> f.getFileName().toString().endsWith("Data.java"))
        .isDirectoryRecursivelyContaining(f -> f.getFileName().toString().endsWith("BaseData.java"));
    assertThat(wsProcDir)
        .isDirectoryRecursivelyContaining(f -> f.getFileName().toString().endsWith("myWebService.java"));
    assertThat(classDir).as("classes are not getting compiled").doesNotExist();
    assertThat(targetClasses).as("classes are not getting compiled").doesNotExist();
  }

  @Test
  public void skipGenerateSources() throws Exception {
    var dataClassDir = generate.project.getBasedir().toPath().resolve("src_dataClasses");
    PathUtils.delete(dataClassDir);

    assertThat(dataClassDir).doesNotExist();

    generate.getMojo().skipGenerateSources = true;
    generate.getMojo().execute();

    assertThat(dataClassDir).doesNotExist();
  }
}
