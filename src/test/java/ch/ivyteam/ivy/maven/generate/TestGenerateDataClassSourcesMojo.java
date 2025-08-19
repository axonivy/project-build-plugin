package ch.ivyteam.ivy.maven.generate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.compile.LocalRepoMojoRule;
import ch.ivyteam.ivy.maven.util.PathUtils;

public class TestGenerateDataClassSourcesMojo {

  @Rule
  public LocalRepoMojoRule<GenerateDataClassSourcesMojo> generate = new LocalRepoMojoRule<>(GenerateDataClassSourcesMojo.GOAL);

  @Test
  public void generateDataClassSources() throws Exception {
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
    assertThat(wsProcDir).doesNotExist();
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
