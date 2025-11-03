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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import ch.ivyteam.ivy.maven.deploy.DeployToEngineMojo.DeployMethod;
import ch.ivyteam.ivy.maven.engine.EngineControl;
import ch.ivyteam.ivy.maven.engine.Slf4jSimpleEngineProperties;
import ch.ivyteam.ivy.maven.extension.ProjectExtension;
import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.maven.test.StartTestEngineMojo;

/**
 * @since 7.1.0
 */
@MojoTest
@ExtendWith(ProjectExtension.class)
class TestDeployToRunningEngine {

  StartTestEngineMojo mojo;
  DeployToEngineMojo deployMojo;

  @BeforeAll
  static void log() {
    Slf4jSimpleEngineProperties.install();
    Slf4jSimpleEngineProperties.enforceSimpleConfigReload();
  }

  @BeforeEach
  @InjectMojo(goal = StartTestEngineMojo.GOAL)
  void startTest(StartTestEngineMojo test) throws Exception {
    this.mojo = test;
    ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest.provideEngine(test);
  }

  @Provides
  MavenProject provideMockedComponent() throws IOException {
    return ProjectExtension.project();
  }

  @Provides
  MavenSession provideSession() {
    var session = Mockito.mock(MavenSession.class);
    System.out.println(session);
    Mockito.lenient().when(session.getUserProperties()).thenReturn(new Properties());
    Mockito.lenient().when(session.getSystemProperties()).thenReturn(new Properties());
    Mockito.lenient().when(session.getSettings()).thenReturn(new Settings());
    return session;
  }

  @BeforeEach
  @InjectMojo(goal = DeployToEngineMojo.GOAL)
  void setup(DeployToEngineMojo deploy) throws Exception {
    deployMojo = deploy;
    TestDeployToEngineMojo.setup(deploy);
    deployMojo.deployToEngineApplication = "MyTestApp";

    deployMojo.deployEngineDirectory = mojo.getEngineDir(mojo.project); // engineDeployVolatile;
    deployMojo.deployTimeoutInSeconds = 120;
    deployMojo.deployFile = Path.of("src/test/resources/deploy-single-10.0.iar");
    deployMojo.deployTestUsers = "true";
  }

  @Test
  void canDeployIar() throws Exception {
    deployMojo.deployToEngineApplication = "Portal";
    var deployedIar = getTarget(deployMojo.deployFile, deployMojo);
    var deployedIarFlagFile = deployedIar.resolveSibling(deployedIar.getFileName() + ".deployed");
    var deployedIarLogFile = deployedIar.resolveSibling(deployedIar.getFileName() + ".deploymentLog");
    Process startedProcess = null;
    try {
      startedProcess = mojo.startEngine();
      deployMojo.execute();
      assertThat(deployedIar).doesNotExist();
      assertThat(deployedIarFlagFile).exists();
      assertThat(deployedIarLogFile).exists();
      assertThat(Assertions.linesOf(deployedIarLogFile)).haveAtLeast(1, new Condition<>(s -> s.contains("Deploying users ..."), ""));
    } finally {
      kill(startedProcess);
    }
  }

  @Test
  void canDeployRemoteIar() throws Exception {
    deployIarRemoteAndAssert();
  }

  @Test
  void canDeployRemoteIar_settingsPassword() throws Exception {
    addServerConnection("admin");
    deployIarRemoteAndAssert();
  }

  @Test
  void canDeployRemoteIar_encryptedSettingsPassword() throws Exception {
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

  private void deployIarRemoteAndAssert() throws Exception {
    deployMojo.deployToEngineApplication = "test";
    deployMojo.deployMethod = DeployMethod.HTTP;

    Process startedProcess = null;
    try {
      startedProcess = mojo.startEngine();
      deployMojo.deployEngineUrl = (String) mojo.project.getProperties()
          .get(EngineControl.Property.TEST_ENGINE_URL);

      LogCollector log = new LogCollector();
      deployMojo.setLog(log);
      deployMojo.execute();

      assertThat(log.getDebug().toString()).contains("Start deploying project(s) of file")
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

  private static void kill(Process startedProcess) throws Exception {
    if (startedProcess != null) {
      CompletableFuture<Process> close = startedProcess.onExit();
      startedProcess.destroy();
      close.get(60, TimeUnit.SECONDS); // properly shut-down; to make file-cleanup work
    }
  }

}
