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

package ch.ivyteam.ivy.maven.engine.deploy;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

public class MarkerFileDeployer implements IvyProjectDeployer
{
  private final File targetEngineDir;
  
  private Log log;
  private DeploymentMarkerFile markerFile;

  public MarkerFileDeployer(File targetEngineDir)
  {
    this.targetEngineDir = targetEngineDir;
  }

  @Override
  @SuppressWarnings("hiding")
  public void deployIar(String iarPath, Log log) throws MojoExecutionException
  {
    File iar = new File(targetEngineDir, iarPath);
    if (!iar.exists() || !iar.isFile())
    {
      log.warn("Skipping deployment of '"+iarPath+"'. The IAR '"+iar+"' does not exist.");
      return;
    }
    
    this.markerFile = new DeploymentMarkerFile(iar);
    this.log = log;
    
    deployInternal();
  }

  private void deployInternal() throws MojoExecutionException
  {
    markerFile.clearAll();
    initDeployment();
    determineDeployResult();
  }

  private void initDeployment() throws MojoExecutionException
  {
    try
    {
      log.info("Deploying project "+markerFile.getDeployCandidate().getName());
      markerFile.doDeploy().createNewFile();
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to initialize engine deployment, could not create marker", ex);
    }
  }

  private void determineDeployResult() throws MojoExecutionException
  {
    try
    {
      wait(()->!markerFile.doDeploy().exists(), 30, TimeUnit.SECONDS);
    }
    catch (TimeoutException ex)
    {
      throw new MojoExecutionException("Deployment result does not exist", ex);
    }
    
    failOnError();
    
    log.info("Deployment finished");
  }

  private void failOnError() throws MojoExecutionException
  {
    if (markerFile.errorLog().exists())
    {
      try
      {
        log.error(FileUtils.readFileToString(markerFile.errorLog()));
      }
      catch (IOException ex)
      {
        log.error("Failed to resolve deployment error cause", ex);
      }
      throw new MojoExecutionException("Deployment of '"+markerFile.getDeployCandidate().getName()+"' failed!");
    }
  }

  private static void wait(Supplier<Boolean> condition, long duration, TimeUnit unit) throws TimeoutException
  {
    long waitMs = unit.toMillis(duration);
    long startMs = System.currentTimeMillis();
    long maxMs = waitMs+startMs;
    while(!condition.get())
    {
      try
      {
        if (System.currentTimeMillis() > maxMs)
        {
          throw new TimeoutException("Operation reached timeout of "+duration+" "+unit);
        }
        Thread.sleep(100);
      }
      catch (InterruptedException ex)
      {
      }
    }
  }
  
}
