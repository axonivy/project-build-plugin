/*
 * Copyright (C) 2018 AXON Ivy AG
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

package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.Executor;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.engine.EngineControl;
import ch.ivyteam.ivy.maven.engine.EngineMojoContext;
import ch.ivyteam.ivy.maven.engine.EngineVmOptions;

/**
 * Starts the Axon.ivy Engine for integration testing.
 *
 * @since 6.2.0
 */
@Mojo(name = StartTestEngineMojo.GOAL)
public class StartTestEngineMojo extends AbstractIntegrationTestMojo
{
  public static final String GOAL = "start-test-engine";
  public static final String IVY_ENGINE_START_TIMEOUT_SECONDS = "ivy.engine.start.timeout.seconds";

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  /** The maximum heap (-Xmx) that is used for starting and running the Engine **/
  @Parameter(property = "ivy.engine.start.maxmem", required = false, defaultValue = "2048m")
  String maxmem;

  /** Additional classpath entries for the JVM that runs the Engine **/
  @Parameter(property = "ivy.engine.start.additional.classpath", required = false, defaultValue = "")
  String additionalClasspath;

  /** Additional options for the JVM that runs the Engine. To modify the classpath or the max heap use the provided properties. **/
  @Parameter(property = "ivy.engine.start.additional.vmoptions", required = false, defaultValue = "")
  String additionalVmOptions;

  /** The file where the engine start is logged **/
  @Parameter(property = "ivy.engine.start.log", required = false, defaultValue = "${project.build.directory}/testEngineOut.log")
  File engineLogFile;

  /** The maximum amount of seconds that we wait for a engine to start */
  @Parameter(property=IVY_ENGINE_START_TIMEOUT_SECONDS, defaultValue="120")
  Integer startTimeoutInSeconds;

  /** Set to <code>true</code> to skip the engine start. */
  @Parameter(property="maven.test.skip", defaultValue="false")
  boolean skipTest;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    if (skipTest)
    {
      getLog().info("Skipping start of engine.");
      return;
    }

    try
    {
      startEngine();
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("Cannot start engine", ex);
    }
  }

  Executor startEngine() throws Exception
  {
    File engineDir = identifyAndGetEngineDirectory();
    
    if (engineToTarget)
    {
      engineDir = copyEngineToTarget(engineDir);
    }
    else
    {
      getLog().info("Using the cached engine from: " + engineDir + " this could lead to unforseen behaviour when executed multiple times."
              + " You can enable <engineToTarget> to copy the cached engine to the project target folder to avoid this.");
    }
    
    EngineVmOptions vmOptions = new EngineVmOptions(maxmem, additionalClasspath, additionalVmOptions);
    EngineControl engineControl = new EngineControl(new EngineMojoContext(
            engineDir, project, getLog(), engineLogFile, vmOptions, startTimeoutInSeconds));
    return engineControl.start();
  }
  
  private File copyEngineToTarget(File cachedEngineDir) throws MojoExecutionException
  {
    try
    {
      File targetEngine = getEngineDir(project);

      getLog().info("Parameter <engineToTarget> is enabled, copying cached engine from: "
              + cachedEngineDir + " to " + targetEngine);

      FileUtils.copyDirectory(cachedEngineDir, targetEngine);
      return targetEngine;
    }
    catch (IOException ex)
    {
      getLog().warn("Could not clone engine from: " + cachedEngineDir);
    }
    return cachedEngineDir;
  }

}
