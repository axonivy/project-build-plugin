/*
 * Copyright (C) 2021 Axon Ivy AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package ch.ivyteam.ivy.maven.deploy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.linesOf;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import org.apache.commons.exec.Executor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;
import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest;
import ch.ivyteam.ivy.maven.ProjectMojoRule;
import ch.ivyteam.ivy.maven.deploy.DeployToEngineMojo.DeployMethod;
import ch.ivyteam.ivy.maven.engine.EngineControl;
import ch.ivyteam.ivy.maven.test.StartTestEngineMojo;

/**
 * @since 7.1.0
 */
public class TestDeployToRunningEngine extends BaseEngineProjectMojoTest {
  StartTestEngineMojo mojo;
  DeployToEngineMojo deployMojo;
  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

  @Before
  public void setup() throws MojoExecutionException {
    mojo = rule.getMojo();
    deployMojo = deployRule.getMojo();
    deployMojo.deployToEngineApplication = "MyTestApp";
    deployMojo.deployEngineDirectory = mojo.getEngineDir(mojo.project);
    deployMojo.deployTimeoutInSeconds = 120;
    deployMojo.deployFile = Path.of("src/test/resources/deploy-single-7.1.0-SNAPSHOT.iar");
    deployMojo.deployTestUsers = "true";

    System.setOut(new PrintStream(outContent));
  }

  @After
  public void restoreStreams() {
    System.setOut(originalOut);
  }

  @Test
  public void canDeployIar() throws Exception {
    deployMojo.deployToEngineApplication = "Portal";
    var deployedIar = getTarget(deployMojo.deployFile, deployMojo);
    var deployedIarFlagFile = deployedIar.resolveSibling(deployedIar.getFileName() + ".deployed");
    var deployedIarLogFile = deployedIar.resolveSibling(deployedIar.getFileName() + ".deploymentLog");
    Executor startedProcess = null;
    try {
      startedProcess = mojo.startEngine();
      deployMojo.execute();
      assertThat(deployedIar).doesNotExist();
      assertThat(deployedIarFlagFile).exists();
      assertThat(deployedIarLogFile).exists();
      assertThat(linesOf(deployedIarLogFile)).haveAtLeast(1, new Condition<>(s -> s.contains("Deploying users ..."), ""));
    } finally {
      kill(startedProcess);
    }
  }

  @Test
  public void canDeployRemoteIar() throws Exception {
    deployIarRemoteAndAssert();
  }

  @Test
  public void canDeployRemoteIar_settingsPassword() throws Exception {
    addServerConnection("admin");
    deployIarRemoteAndAssert();
  }

  @Test
  public void canDeployRemoteIar_encryptedSettingsPassword() throws Exception {
    addServerConnection("{VUpeDRRbfD4Hmk9WLKzhqLkLttTCsWfLtr75Nt9K/3k=}");
    System.setProperty("settings.security",
            TestDeployToRunningEngine.class.getResource("settings-security.xml").getPath());
    deployIarRemoteAndAssert();
  }

  private void addServerConnection(String password) {
    Server server = new Server();
    server.setId("test.server");
    server.setUsername("admin");
    server.setPassword(password);
    deployMojo.session.getSettings().addServer(server);
    deployMojo.deployServerId = "test.server";
  }

  private void deployIarRemoteAndAssert() throws Exception, MojoExecutionException, MojoFailureException {
    deployMojo.deployToEngineApplication = "test";
    deployMojo.deployMethod = DeployMethod.HTTP;

    Executor startedProcess = null;
    try {
      System.setOut(originalOut);
      startedProcess = mojo.startEngine();
      deployMojo.deployEngineUrl = (String) rule.project.getProperties()
              .get(EngineControl.Property.TEST_ENGINE_URL);
      System.setOut(new PrintStream(outContent));

      deployMojo.execute();

      assertThat(outContent.toString()).contains("Start deploying project(s) of file")
              .contains("Application: test")
              .contains("Deploying users ...")
              .doesNotContain("deployDirectory is set but will not be used for HTTP Deployment.");
    } finally {
      kill(startedProcess);
    }
  }

  private static Path getTarget(Path iar, DeployToEngineMojo mojo) {
    return mojo.deployEngineDirectory
            .resolve(mojo.deployDirectory)
            .resolve(mojo.deployToEngineApplication)
            .resolve(iar.getFileName().toString());
  }

  private static void kill(Executor startedProcess) {
    if (startedProcess != null) {
      startedProcess.getWatchdog().destroyProcess();
    }
  }

  @Rule
  public RunnableEngineMojoRule<StartTestEngineMojo> rule = new RunnableEngineMojoRule<StartTestEngineMojo>(
          StartTestEngineMojo.GOAL);

  @Rule
  public ProjectMojoRule<DeployToEngineMojo> deployRule = new ProjectMojoRule<DeployToEngineMojo>(
          Path.of("src/test/resources/base"), DeployToEngineMojo.GOAL);

}
