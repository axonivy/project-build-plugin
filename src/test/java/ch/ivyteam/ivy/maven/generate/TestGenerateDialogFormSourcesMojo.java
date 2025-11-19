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
class TestGenerateDialogFormSourcesMojo {

  private GenerateDialogFormSourcesMojo mojo;

  @BeforeEach
  @InjectMojo(goal = InstallEngineMojo.GOAL)
  void setUpEngine(InstallEngineMojo install) throws Exception {
    BaseEngineProjectMojoTest.provideEngine(install);
  }

  @BeforeEach
  @InjectMojo(goal = GenerateDialogFormSourcesMojo.GOAL)
  void setUp(GenerateDialogFormSourcesMojo form) throws Exception {
    this.mojo = form;
    BaseEngineProjectMojoTest.provideEngine(mojo);
    mojo.localRepository = LocalRepoTest.repo();
  }

  @Provides
  MavenProject provideMockedComponent() throws IOException {
    return ProjectExtension.project();
  }

  @Test
  void generateFormSources() throws Exception {
    var targetSrcHd = mojo.project.getBasedir().toPath().resolve("target").resolve("src_hd");
    PathUtils.delete(targetSrcHd);

    assertThat(targetSrcHd).doesNotExist();

    mojo.execute();

    assertThat(targetSrcHd)
        .isDirectoryRecursivelyContaining(f -> f.getFileName().toString().endsWith("myForm.xhtml"));
  }

  @Test
  void skipGenerateSources() throws Exception {
    var targetSrcHd = mojo.project.getBasedir().toPath().resolve("target").resolve("src_hd");
    PathUtils.delete(targetSrcHd);

    assertThat(targetSrcHd).doesNotExist();

    mojo.skipGenerateSources = true;
    mojo.execute();

    assertThat(targetSrcHd).doesNotExist();
  }
}
