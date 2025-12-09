package ch.ivyteam.ivy.maven.generate;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.util.PathUtils;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestGenerateDataClassSourcesMojo {

  private GenerateDataClassSourcesMojo mojo;

  @BeforeEach
  @InjectMojo(goal = GenerateDataClassSourcesMojo.GOAL)
  void setUp(GenerateDataClassSourcesMojo data) {
    this.mojo = data;
  }

  @Test
  void generateDataClassSources() throws Exception {
    var projectDir = mojo.project.getBasedir().toPath();
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

    mojo.execute();

    assertThat(dataClassDir)
        .isDirectoryRecursivelyContaining(f -> f.getFileName().toString().endsWith("Data.java"))
        .isDirectoryRecursivelyContaining(f -> f.getFileName().toString().endsWith("BaseData.java"));
    assertThat(wsProcDir).doesNotExist();
    assertThat(classDir).as("classes are not getting compiled").doesNotExist();
    assertThat(targetClasses).as("classes are not getting compiled").doesNotExist();
  }

  @Test
  void skipGenerateSources() throws Exception {
    var dataClassDir = mojo.project.getBasedir().toPath().resolve("src_dataClasses");
    PathUtils.delete(dataClassDir);

    assertThat(dataClassDir).doesNotExist();

    mojo.skipGenerateSources = true;
    mojo.execute();

    assertThat(dataClassDir).doesNotExist();
  }
}
