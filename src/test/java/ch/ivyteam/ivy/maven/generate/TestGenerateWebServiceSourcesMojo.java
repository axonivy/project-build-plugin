package ch.ivyteam.ivy.maven.generate;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest;
import ch.ivyteam.ivy.maven.InstallEngineMojo;
import ch.ivyteam.ivy.maven.extension.LocalRepoTest;
import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.util.PathUtils;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestGenerateWebServiceSourcesMojo {

  private GenerateWebServiceSourcesMojo mojo;

  @BeforeEach
  @InjectMojo(goal = InstallEngineMojo.GOAL)
  void setUpEngine(InstallEngineMojo install) throws Exception {
    BaseEngineProjectMojoTest.provideEngine(install);
  }

  @BeforeEach
  @InjectMojo(goal = GenerateWebServiceSourcesMojo.GOAL)
  void setUp(GenerateWebServiceSourcesMojo soap) throws Exception {
    this.mojo = soap;
    BaseEngineProjectMojoTest.provideEngine(mojo);
    mojo.localRepository = LocalRepoTest.repo();
  }

  @Provides
  MavenProject provideMockedComponent() throws IOException {
    return ProjectExtension.project();
  }

  @Test
  void generateWebServiceSources() throws Exception {
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

    assertThat(dataClassDir).doesNotExist();
    assertThat(wsProcDir)
        .isDirectoryRecursivelyContaining(f -> f.getFileName().toString().endsWith("myWebService.java"));
    assertThat(classDir).as("classes are not getting compiled").doesNotExist();
    assertThat(targetClasses).as("classes are not getting compiled").doesNotExist();
  }

  @Test
  void skipGenerateSources() throws Exception {
    var wsProcDir = mojo.project.getBasedir().toPath().resolve("src_wsproc");
    PathUtils.delete(wsProcDir);

    assertThat(wsProcDir).doesNotExist();

    mojo.skipGenerateSources = true;
    mojo.execute();

    assertThat(wsProcDir).doesNotExist();
  }
}
