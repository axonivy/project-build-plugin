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
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.maven.engine.deploy.dir.DeploymentFiles;
import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.maven.util.PathUtils;

@MojoTest
class TestDeployToEngineMojo {

  @Test
  @InjectMojo(goal = DeployToEngineMojo.GOAL)
  void deployPackedIar(DeployToEngineMojo deploy) throws Throwable {
    DeployToEngineMojo mojo = deploy;
    setup(mojo);

    var deployedIar = getTarget(mojo.deployFile, mojo);
    var deploymentOptionsFile = deployedIar.resolveSibling(deployedIar.getFileName() + ".options.yaml");

    assertThat(deployedIar).doesNotExist();
    assertThat(deploymentOptionsFile).doesNotExist();

    DelayedOperation mockEngineDeployThread = new DelayedOperation(500, TimeUnit.MILLISECONDS);
    Callable<Void> engineOperation = () -> {
      assertThat(deploymentOptionsFile).doesNotExist();
      assertThat(deployedIar).exists();
      Files.delete(deployedIar);
      return null;
    };
    mockEngineDeployThread.execute(engineOperation);
    mojo.execute();
    mockEngineDeployThread.failOnException();

    assertThat(deployedIar)
        .as("IAR should not exist in engine deploy directory")
        .doesNotExist();
  }

  @Test
  @InjectMojo(goal = DeployToEngineMojo.GOAL)
  void deployWithExistingOptionsFile(DeployToEngineMojo deploy) throws Throwable {
    DeployToEngineMojo mojo = deploy;
    setup(mojo);
    mojo.project.getProperties().setProperty("doDeploy.test.user", "true");

    mojo.deployOptionsFile = Path.of("src/test/resources/options.yaml");
    var deployedIar = getTarget(mojo.deployFile, mojo);
    var deploymentOptionsFile = deployedIar.resolveSibling(deployedIar.getFileName() + ".options.yaml");

    assertThat(deployedIar).doesNotExist();
    assertThat(deploymentOptionsFile).doesNotExist();

    DelayedOperation mockEngineDeployThread = new DelayedOperation(500, TimeUnit.MILLISECONDS);
    Callable<Void> engineOperation = () -> {
      assertThat(deploymentOptionsFile).exists();
      assertThat(deploymentOptionsFile).hasContent("deployTestUsers: true\ntarget: AUTO");
      Files.delete(deploymentOptionsFile);
      assertThat(deployedIar).exists();
      Files.delete(deployedIar);
      return null;
    };
    mockEngineDeployThread.execute(engineOperation);
    mojo.execute();
    mockEngineDeployThread.failOnException();

    assertThat(deployedIar)
        .as("IAR should not exist in engine deploy directory")
        .doesNotExist();
  }

  @Provides
  MavenProject provideMockedComponent() {
    var project = new MavenProject();
    project.setGroupId("com.acme");
    project.setArtifactId("test.project");
    project.setVersion("1.0.0-SNAPSHOT");
    project.getBuild().setDirectory("target");
    return project;
  }

  @Test
  @InjectMojo(goal = DeployToEngineMojo.GOAL)
  void deployWithOptions(DeployToEngineMojo deploy) throws Throwable {
    DeployToEngineMojo mojo = deploy;
    setup(mojo);

    mojo.deployTestUsers = "true";
    mojo.deployTargetVersion = "RELEASED";
    mojo.deployTargetState = "INACTIVE";
    mojo.deployTargetFileFormat = "EXPANDED";

    var deployedIar = getTarget(mojo.deployFile, mojo);
    var deploymentOptionsFile = deployedIar.resolveSibling(deployedIar.getFileName() + ".options.yaml");

    assertThat(deployedIar).doesNotExist();
    assertThat(deploymentOptionsFile).doesNotExist();

    DelayedOperation mockEngineDeployThread = new DelayedOperation(500, TimeUnit.MILLISECONDS);
    Callable<Void> engineOperation = () -> {
      assertThat(deploymentOptionsFile).exists();
      assertThat(deploymentOptionsFile).hasContent(
          """
            deployTestUsers: "TRUE"
            target:
              version: RELEASED
              state: INACTIVE
              fileFormat: EXPANDED""");
      Files.delete(deploymentOptionsFile);
      assertThat(deployedIar).exists();
      Files.delete(deployedIar);
      return null;
    };
    mockEngineDeployThread.execute(engineOperation);
    mojo.execute();
    mockEngineDeployThread.failOnException();

    assertThat(deployedIar)
        .as("IAR should not exist in engine deploy directory")
        .doesNotExist();
  }

  @Test
  @InjectMojo(goal = DeployToEngineMojo.GOAL)
  void failOnEngineDeployError(DeployToEngineMojo deploy) throws Throwable {
    DeployToEngineMojo mojo = deploy;
    setup(mojo);
    DeploymentFiles markers = new DeploymentFiles(getTarget(mojo.deployFile, mojo));
    var deployedIar = getTarget(mojo.deployFile, mojo);

    DelayedOperation mockEngineDeployThread = new DelayedOperation(500, TimeUnit.MILLISECONDS);
    Callable<Void> engineOperation = () -> {
      Files.writeString(markers.errorLog(), "validation errors");
      assertThat(deployedIar).exists();
      Files.delete(deployedIar);
      return null;
    };

    mojo.setLog(new LogCollector()); // do not print errors to console (avoid Maven build assumes error)
    mockEngineDeployThread.execute(engineOperation);
    try {
      mojo.execute();
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    } catch (MojoExecutionException ex) {
      assertThat(ex).hasMessageContaining("failed!");
    } finally {
      mockEngineDeployThread.failOnException();
    }
  }

  private static Path getTarget(Path iar, DeployToEngineMojo mojo) {
    return mojo.deployEngineDirectory
        .resolve(mojo.deployDirectory)
        .resolve(mojo.deployToEngineApplication)
        .resolve(iar.getFileName().toString());
  }

  static void setup(DeployToEngineMojo mojo) throws IOException {
    mojo.deployEngineDirectory = createEngineDir();
    mojo.deployToEngineApplication = "TestApp";

    try {
      mojo.deployFile.toFile().getParentFile().mkdir();
      mojo.deployFile.toFile().createNewFile();
    } catch (IOException ex) {
      System.err.println("Failed to create IAR @ " + mojo.deployFile.toAbsolutePath());
      throw ex;
    }
  }

  private static Path createEngineDir() throws IOException {
    var engine = Path.of("target").resolve("myTestIvyEngine");
    var deploy = engine.resolve("deploy");
    Files.createDirectories(deploy);
    return engine.toAbsolutePath();
  }

  public static void cleanup(DeployToEngineMojo mojo) {
    PathUtils.delete(mojo.deployEngineDirectory);
  }

  private static class DelayedOperation {
    private final long delayMillis;
    private Throwable throwable;
    private Thread thread;

    public DelayedOperation(long delay, TimeUnit unit) {
      delayMillis = unit.toMillis(delay);
    }

    public void execute(Callable<Void> delayedFunction) {
      thread = new Thread(){
        @Override
        public void run() {
          try {
            Thread.sleep(delayMillis);
            delayedFunction.call();
          } catch (Throwable functionThrowable) {
            functionThrowable.printStackTrace();
            DelayedOperation.this.throwable = functionThrowable;
          }
        }
      };
      thread.start();
    }

    public void failOnException() throws Throwable {
      assertThat(thread).as("Delayed operation thread has never been started.").isNotNull();
      while (thread.isAlive()) {
        Thread.sleep(10); // wait for result
      }
      if (throwable != null) {
        throw throwable;
      }
    }
  }

}
