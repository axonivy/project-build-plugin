/*
 * Copyright (C) 2021 Axon Ivy AG
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
package ch.ivyteam.ivy.maven.engine.deploy.dir;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

public class FileDeployer implements IvyDeployer
{
  private final File deployDir;
  private final Integer timeoutInSeconds;

  private Log log;
  private DeploymentFiles deploymentFiles;

  private File deployFile;
  private File targetDeployableFile;
  private File deploymentOptionsFile;

  public FileDeployer(File deployDir, File deploymentOptions, Integer deployTimeoutInSeconds, File deployFile, File targetDeployableFile)
  {
    this.deployDir = deployDir;
    this.deploymentOptionsFile = deploymentOptions;
    this.timeoutInSeconds = deployTimeoutInSeconds;

    this.deployFile = deployFile;
    this.targetDeployableFile = targetDeployableFile;
  }

  @Override
  @SuppressWarnings("hiding")
  public void deploy(String deployableFilePath, Log log) throws MojoExecutionException
  {
    File deployableFile = new File(deployDir, deployableFilePath);
    this.deploymentFiles = new DeploymentFiles(deployableFile);
    this.log = log;

    deployInternal();
  }

  private void deployInternal() throws MojoExecutionException
  {
    clear();
    initDeployment();
    copyDeployableToEngine();
    determineDeployResult();
  }


  private void clear()
  {
    deploymentFiles.clearAll();
  }

  private void initDeployment() throws MojoExecutionException
  {
    try
    {
      if (deploymentOptionsFile != null)
      {
        File engineOption = new File(deploymentFiles.getDeployCandidate().getParentFile(), deploymentOptionsFile.getName());
        FileUtils.copyFile(deploymentOptionsFile, engineOption);
      }
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to initialize engine deployment, could not copy options file", ex);
    }
  }

  private void copyDeployableToEngine() throws MojoExecutionException
  {
    try
    {
      log.info("Uploading file " + deployFile + " to " + targetDeployableFile);
      FileUtils.copyFile(deployFile, targetDeployableFile);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Upload of file '"+deployFile.getName()+"' to engine failed.", ex);
    }
  }

  private void determineDeployResult() throws MojoExecutionException
  {
    FileLogForwarder logForwarder = new FileLogForwarder(deploymentFiles.log(), log, new EngineLogLineHandler(log));
    try
    {
      logForwarder.activate();
      log.debug("Deployment candidate " + deploymentFiles.getDeployCandidate());
      wait(()->!deploymentFiles.getDeployCandidate().exists(), timeoutInSeconds, TimeUnit.SECONDS);
    }
    catch (TimeoutException ex)
    {
      throw new MojoExecutionException("Deployment result does not exist", ex);
    }
    finally
    {
      logForwarder.deactivate();
    }

    failOnError();
    log.info("Deployment finished");
  }

  private void failOnError() throws MojoExecutionException
  {
    if (deploymentFiles.errorLog().exists())
    {
      try
      {
        log.error(FileUtils.readFileToString(deploymentFiles.errorLog()));
      }
      catch (IOException ex)
      {
        log.error("Failed to resolve deployment error cause", ex);
      }
      throw new MojoExecutionException("Deployment of '"+deploymentFiles.getDeployCandidate().getName()+"' failed!");
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
