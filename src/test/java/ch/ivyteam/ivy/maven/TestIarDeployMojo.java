/*
 * Copyright (C) 2016 AXON IVY AG
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

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.failBecauseExceptionWasNotThrown;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.engine.deploy.DeploymentMarkerFiles;

public class TestIarDeployMojo
{
  @Test
  public void deployPackedIar() throws Exception
  {
    IarDeployMojo mojo = rule.getMojo();
    File iar = mojo.deployIarFile;
    assertThat(iar).isNotNull();

    iar.createNewFile();
    File deployedIar = getTarget(iar, mojo);
    File deployMarkerFile = new DeploymentMarkerFiles(deployedIar).doDeploy();
    assertThat(deployedIar).doesNotExist();
    assertThat(deployMarkerFile).doesNotExist();
    
    DelayedOperation mockEngineDeployThread = new DelayedOperation(500, TimeUnit.MILLISECONDS);
    Callable<Void> engineOperation = () -> {
      assertThat(deployMarkerFile).as("deployment must be initialized").exists();
      deployMarkerFile.delete(); //deployment finished
      return null;
    };
    mockEngineDeployThread.execute(engineOperation);
    mojo.execute();
    mockEngineDeployThread.failOnExecption();
    
    assertThat(deployedIar)
      .as("IAR must exist in engine deploy directory")
      .exists();
  }
  
  @Test
  public void failOnEngineDeployError() throws Exception
  {
    IarDeployMojo mojo = rule.getMojo();
    mojo.deployIarFile.createNewFile();
    DeploymentMarkerFiles markers = new DeploymentMarkerFiles(getTarget(mojo.deployIarFile, mojo));
    
    DelayedOperation mockEngineDeployThread = new DelayedOperation(500, TimeUnit.MILLISECONDS);
    Callable<Void> engineOperation = () -> {
      assertThat(markers.doDeploy()).as("deployment must be initialized").exists();
      FileUtils.write(markers.errorLog(), "validation errors");
      markers.doDeploy().delete(); //deployment finished
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
      mockEngineDeployThread.failOnExecption();
    }
  }

  private static File getTarget(File iar, IarDeployMojo mojo)
  {
    File deploy = new File(mojo.deployEngineDirectory, mojo.deployDirectory);
    File app = new File(deploy, mojo.deployToEngineApplication);
    File deployedIar = new File(app, iar.getName());
    return deployedIar;
  }

  @Rule
  public ProjectMojoRule<IarDeployMojo> rule = new ProjectMojoRule<IarDeployMojo>(
          new File("src/test/resources/base"), IarDeployMojo.GOAL)
  {
    @Override
    protected void before() throws Throwable 
    {
      super.before();
      getMojo().deployEngineDirectory = createEngineDir();
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
    private Exception ex;
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
          catch (Exception functionEx)
          {
            DelayedOperation.this.ex = functionEx;
          }
        }
      };
      thread.start();
    }
  
    public void failOnExecption() throws Exception
    {
      assertThat(thread).as("Delayed operation thread has never been started.").isNotNull();
      while(thread.isAlive())
      {
        Thread.sleep(10); // wait for result
      }
      if (ex != null)
      {
        throw ex;
      }
    }
  }

}
