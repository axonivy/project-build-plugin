/*
 * Copyright (C) 2016 AXON IVY AG
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
 * @since 6.1.1
 */
@Mojo(name = StopEngineMojo.GOAL)
public class StopEngineMojo extends AbstractEngineMojo
{
  public static final String GOAL = "stop-engine";
  
  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  @Parameter(property = "ivy.engine.stop.maxmem", required = false, defaultValue = "128m")
  String maxmem;
  
  @Parameter(property = "ivy.engine.stop.additional.classpath", required = false, defaultValue = "")
  String additionalClasspath;
  
  @Parameter(property = "ivy.engine.stop.additional.vmoptions", required = false, defaultValue = "")
  String additionalVmOptions;
  
  @Parameter(property = "ivy.engine.stop.log", required = false, defaultValue = "${project.build.directory}/testEngineOut.log")
  File engineLogFile;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    try
    {
      EngineVmOptions vmOptions = new EngineVmOptions(maxmem, additionalClasspath, additionalVmOptions);
      EngineControl engineControl = new EngineControl(new EngineMojoContext(getEngineDirectory(), project, getLog(), engineLogFile, vmOptions));
      engineControl.stop();
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("Cannot Stop engine", ex);
    }
  }
}
