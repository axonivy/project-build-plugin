/*
 * Copyright (C) 2018 AXON Ivy AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.engine.deploy.dir.DeploymentFiles;

public class TestDeployToEngineMojo
{
  @Test
  public void deployPackedIar() throws Throwable
  {
    DeployToEngineMojo mojo = rule.getMojo();

    File deployedIar = getTarget(mojo.deployFile, mojo);
    File deploymentOptionsFile = new File(deployedIar.getParentFile(), deployedIar.getName()+"options.yaml");

    assertThat(deployedIar).doesNotExist();
    assertThat(deploymentOptionsFile).doesNotExist();

    DelayedOperation mockEngineDeployThread = new DelayedOperation(500, TimeUnit.MILLISECONDS);
    Callable<Void> engineOperation = () -> {
      assertThat(deploymentOptionsFile).doesNotExist();
      assertThat(deployedIar).exists();
      deployedIar.delete();
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
  public void deployWithExistingOptionsFile() throws Throwable
  {
    rule.project.getProperties().setProperty("doDeploy.test.user", "true");
    DeployToEngineMojo mojo = rule.getMojo();

    mojo.deployOptionsFile = new File("src/test/resources/options.yaml");
    File deployedIar = getTarget(mojo.deployFile, mojo);
    File deploymentOptionsFile = new File(deployedIar.getParentFile(), deployedIar.getName()+".options.yaml");

    assertThat(deployedIar).doesNotExist();
    assertThat(deploymentOptionsFile).doesNotExist();

    DelayedOperation mockEngineDeployThread = new DelayedOperation(500, TimeUnit.MILLISECONDS);
    Callable<Void> engineOperation = () -> {
      assertThat(deploymentOptionsFile).exists();
      assertThat(deploymentOptionsFile).hasContent("deployTestUsers: true\ntarget: AUTO");
      deploymentOptionsFile.delete();
      assertThat(deployedIar).exists();
      Files.delete(deployedIar.toPath());
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
  public void deployWithOptions() throws Throwable
  {
    DeployToEngineMojo mojo = rule.getMojo();
    mojo.deployTestUsers = "true";
    mojo.deployConfigOverwrite = true;
    mojo.deployConfigCleanup = "REMOVE_ALL";
    mojo.deployTargetVersion = "RELEASED";
    mojo.deployTargetState = "INACTIVE";
    mojo.deployTargetFileFormat = "EXPANDED";

    File deployedIar = getTarget(mojo.deployFile, mojo);
    File deploymentOptionsFile = new File(deployedIar.getParentFile(), deployedIar.getName()+".options.yaml");

    assertThat(deployedIar).doesNotExist();
    assertThat(deploymentOptionsFile).doesNotExist();

    DelayedOperation mockEngineDeployThread = new DelayedOperation(500, TimeUnit.MILLISECONDS);
    Callable<Void> engineOperation = () -> {
      assertThat(deploymentOptionsFile).exists();
      assertThat(deploymentOptionsFile).hasContent(
              "deployTestUsers: TRUE\n" +
              "configuration:\n" +
              "  overwrite: true\n" +
              "  cleanup: REMOVE_ALL\n" +
              "target:\n" +
              "  version: RELEASED\n" +
              "  state: INACTIVE\n"+
              "  fileFormat: EXPANDED");
      deploymentOptionsFile.delete();
      assertThat(deployedIar).exists();
      Files.delete(deployedIar.toPath());
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
  public void failOnEngineDeployError() throws Throwable
  {
    DeployToEngineMojo mojo = rule.getMojo();
    DeploymentFiles markers = new DeploymentFiles(getTarget(mojo.deployFile, mojo));
    File deployedIar = getTarget(mojo.deployFile, mojo);

    DelayedOperation mockEngineDeployThread = new DelayedOperation(500, TimeUnit.MILLISECONDS);
    Callable<Void> engineOperation = () -> {
      FileUtils.write(markers.errorLog(), "validation errors");
      assertThat(deployedIar).exists();
      deployedIar.delete();
      return null;
    };

    mockEngineDeployThread.execute(engineOperation);
    try
    {
      mojo.execute();
      failBecauseExceptionWasNotThrown(MojoExecutionException.class);
    }
    catch (MojoExecutionException ex)
    {
      assertThat(ex).hasMessageContaining("failed!");
    }
    finally
    {
      mockEngineDeployThread.failOnException();
    }
  }
  
  private static File getTarget(File iar, DeployToEngineMojo mojo)
  {
    File deploy = new File(mojo.deployEngineDirectory, mojo.deployDirectory);
    File app = new File(deploy, mojo.deployToEngineApplication);
    File deployedIar = new File(app, iar.getName());
    return deployedIar;
  }

  @Rule
  public ProjectMojoRule<DeployToEngineMojo> rule = new ProjectMojoRule<DeployToEngineMojo>(
          new File("src/test/resources/base"), DeployToEngineMojo.GOAL)
  {
    @Override
    protected void before() throws Throwable
    {
      super.before();

      getMojo().deployEngineDirectory = createEngineDir();

      try
      {
        getMojo().deployFile.getParentFile().mkdir();
        getMojo().deployFile.createNewFile();
      }
      catch (IOException ex)
      {
        System.err.println("Failed to create IAR @ "+getMojo().deployFile.getAbsolutePath());
        throw ex;
      }
    }

    private File createEngineDir()
    {
      File engine = new File("target/myTestIvyEngine");
      File deploy = new File(engine, "deploy");
      deploy.mkdirs();
      return engine.getAbsoluteFile();
    }

    @Override
    protected void after()
    {
      super.after();
      FileUtils.deleteQuietly(getMojo().deployEngineDirectory);
    }
  };

  private static class DelayedOperation
  {
    private final long delayMillis;
    private Throwable throwable;
    private Thread thread;

    public DelayedOperation(long delay, TimeUnit unit)
    {
      delayMillis = unit.toMillis(delay);
    }

    public void execute(Callable<Void> delayedFunction)
    {
      thread = new Thread(){
        @Override
        public void run()
        {
          try
          {
            Thread.sleep(delayMillis);
            delayedFunction.call();
          }
          catch (Throwable functionThrowable)
          {
            functionThrowable.printStackTrace();
            DelayedOperation.this.throwable = functionThrowable;
          }
        }
      };
      thread.start();
    }

    public void failOnException() throws Throwable
    {
      assertThat(thread).as("Delayed operation thread has never been started.").isNotNull();
      while(thread.isAlive())
      {
        Thread.sleep(10); // wait for result
      }
      if (throwable != null)
      {
        throw throwable;
      }
    }
  }

}
