package ch.ivyteam.ivy.maven.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest;
import ch.ivyteam.ivy.maven.engine.EngineControl;
import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.maven.test.AbstractIntegrationTestMojo.TestEngineLocation;

public class TestEngineControlEngineDirectory extends BaseEngineProjectMojoTest {
  @Rule
  public RunnableEngineMojoRule<StopTestEngineMojo> rule = new RunnableEngineMojoRule<StopTestEngineMojo>(
          StopTestEngineMojo.GOAL);

  @Test
  public void engineControl_engineDir_doesNotExist() throws Exception {
    LogCollector log = new LogCollector();
    StopTestEngineMojo mojo = rule.getMojo();
    mojo.setLog(log);
    mojo.testEngine = TestEngineLocation.MODIFY_EXISTING;

    EngineControl controller = mojo.createEngineController();
    controller.stop();
    assertThat(log.getWarnings()).isEmpty();
    assertThat(log.getErrors()).isEmpty();
  }

  @Test
  public void engineControl_engineDir_isNull() throws Exception {
    StopTestEngineMojo mojo = new StopTestEngineMojo();
    mojo.project = rule.project;
    mojo.engineDirectory = rule.getMojo().engineCacheDirectory;

    assertThat(mojo.createEngineController()).isNotNull();
  }
}
