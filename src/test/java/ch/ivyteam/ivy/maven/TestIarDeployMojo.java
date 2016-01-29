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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.engine.deploy.MarkerFileDeployer;

public class TestIarDeployMojo
{
  @Test
  public void deployPackedIar() throws Exception
  {
    IarDeployMojo mojo = rule.getMojo();
    mojo.deployEngineDirectory = createEngineDir();
    File iar = mojo.deployIarFile;
    assertThat(iar).isNotNull();

    iar.createNewFile();
    File deploy = new File(mojo.deployEngineDirectory, mojo.deployDirectory);
    File app = new File(deploy, mojo.deployToEngineApplication);
    File deployedIar = new File(app, iar.getName());
    File deployMarkerFile = new MarkerFileDeployer.DeploymentMarkerFile(deployedIar).doDeploy();
    assertThat(deployedIar).doesNotExist();
    assertThat(deployMarkerFile).doesNotExist();
    
    DelayedOperation mockEngineDeployThread = new DelayedOperation(500, TimeUnit.MILLISECONDS);
    mockEngineDeployThread.execute(() -> {
      assertThat(deployMarkerFile).exists(); //start deploying
      deployMarkerFile.delete(); //deployment finished
    });
    
    mojo.execute();

    mockEngineDeployThread.failOnExecption();
    
    assertThat(deployedIar)
      .as("IAR must exist in engine deploy directory")
      .exists();
  }
  
  private static File createEngineDir() throws IOException
  {
    File engine = Files.createTempDirectory("myIvyEngine").toFile();
    File deploy = new File(engine, "deploy");
    deploy.mkdir();
    return engine;
  }

  @Rule
  public ProjectMojoRule<IarDeployMojo> rule = new ProjectMojoRule<IarDeployMojo>(
          new File("src/test/resources/base"), IarDeployMojo.GOAL);

  private static class DelayedOperation
  {
    private final long delayMillis;
    private Exception ex;
    private Thread thread;
  
    public DelayedOperation(long delay, TimeUnit unit)
    {
      delayMillis = unit.toMillis(delay);
    }
    
    public void execute(Runnable delayedFunction)
    {
      thread = new Thread(){
        @Override
        public void run()
        {
          try
          {
            Thread.sleep(delayMillis);
            delayedFunction.run();
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
