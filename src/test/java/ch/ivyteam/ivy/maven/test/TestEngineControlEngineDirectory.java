package ch.ivyteam.ivy.maven.test;

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
import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.maven.test.AbstractIntegrationTestMojo.TestEngineLocation;

@MojoTest
@ExtendWith(ProjectExtension.class)
class TestEngineControlEngineDirectory {

  private StopTestEngineMojo mojo;

  @BeforeEach
  @InjectMojo(goal = InstallEngineMojo.GOAL)
  void setUpEngine(InstallEngineMojo install) throws Exception {
    BaseEngineProjectMojoTest.provideEngine(install);
  }

  @BeforeEach
  @InjectMojo(goal = StopTestEngineMojo.GOAL)
  void setUp(StopTestEngineMojo stop) throws Exception {
    this.mojo = stop;
    ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest.provideEngine(mojo);
  }

  @Provides
  MavenProject provideMockedComponent() throws IOException {
    return ProjectExtension.project();
  }

  @Test
  void engineControl_engineDir_doesNotExist() throws Exception {
    LogCollector log = new LogCollector();
    mojo.setLog(log);
    mojo.testEngine = TestEngineLocation.MODIFY_EXISTING;

    var controller = mojo.createEngineController();
    controller.stop();
    assertThat(log.getWarnings()).isEmpty();
    assertThat(log.getErrors()).isEmpty();
  }

  @Test
  void engineControl_engineDir_isNull() throws Exception {
    mojo.project = mojo.project;
    mojo.engineDirectory = mojo.engineCacheDirectory;
    assertThat(mojo.createEngineController()).isNotNull();
  }
}
