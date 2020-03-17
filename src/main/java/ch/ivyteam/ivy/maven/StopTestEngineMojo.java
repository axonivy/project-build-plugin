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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.engine.EngineControl;
import ch.ivyteam.ivy.maven.engine.EngineMojoContext;
import ch.ivyteam.ivy.maven.engine.EngineVmOptions;

/**
 * Stops the Axon.ivy Engine after integration testing
 * 
 * @since 6.2.0
 */
@Mojo(name = StopTestEngineMojo.GOAL)
public class StopTestEngineMojo extends AbstractIntegrationTestMojo
{
  public static final String GOAL = "stop-test-engine";
  
  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  /** The maximum heap (-Xmx) that is used for stopping the Engine **/
  @Parameter(property = "ivy.engine.stop.maxmem", required = false, defaultValue = "128m")
  String maxmem;
  
  /** Additional classpath entries for the JVM that stops the Engine **/
  @Parameter(property = "ivy.engine.stop.additional.classpath", required = false, defaultValue = "")
  String additionalClasspath;
  
  /** Additional options for the JVM that stops the Engine. To modify the classpath or the max heap use the provided properties. **/
  @Parameter(property = "ivy.engine.stop.additional.vmoptions", required = false, defaultValue = "")
  String additionalVmOptions;
  
  /** The maximum amount of seconds that we wait for a engine to stop */
  @Parameter(property="ivy.engine.stop.timeout.seconds", defaultValue="45")
  Integer stopTimeoutInSeconds;
  
  /** Set to <code>true</code> to skip the engine stop. */
  @Parameter(property="maven.test.skip", defaultValue="false")
  boolean skipTest;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    if (skipTest)
    {
      getLog().info("Skipping stop of engine.");
      return;
    }
    
    try
    {
      createEngineController().stop();
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("Cannot Stop engine", ex);
    }
  }

  public EngineControl createEngineController() throws MojoExecutionException
  {
    File engineDir = getEngineDir(project);
    if (engineDir == null || !engineDir.exists())
    {
      engineDir = identifyAndGetEngineDirectory();
    }
    
    EngineVmOptions vmOptions = new EngineVmOptions(maxmem, additionalClasspath, additionalVmOptions);
    EngineControl engineControl = new EngineControl(new EngineMojoContext(
            engineDir, project, getLog(), null, vmOptions, stopTimeoutInSeconds));
    return engineControl;
  }
  
}
