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

package ch.ivyteam.ivy.maven.engine;

import java.io.File;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.util.ClasspathJar;
import ch.ivyteam.ivy.maven.util.SharedFile;

public class EngineMojoContext
{
  public final File engineDirectory;
  public final MavenProject project;
  public final Log log;
  public final EngineVmOptions vmOptions;
  public final String engineClasspathJarPath;
  public final MavenProperties properties;
  public final File engineLogFile;
  public final Integer timeoutInSeconds;

  public EngineMojoContext(File engineDirectory, MavenProject project, Log log, File engineLogFile, EngineVmOptions vmOptions, Integer timeoutInSeconds)
  {
    this.engineDirectory = engineDirectory;
    this.project = project;
    this.log = log;
    this.engineLogFile = engineLogFile;
    this.vmOptions = vmOptions;
    this.timeoutInSeconds = timeoutInSeconds;
    
    this.properties = new MavenProperties(project, log);
    this.engineClasspathJarPath = setupEngineClasspathJarIfNotExists();

    if (!(new File(engineClasspathJarPath).exists()))
    {
      throw new RuntimeException("Engine ClasspathJar " + engineClasspathJarPath + " does not exist.");
    }
    if (!(engineDirectory.exists()))
    {
      throw new RuntimeException("Engine Directory " + engineDirectory + " does not exist.");
    }
  }
  
  private String setupEngineClasspathJarIfNotExists()
  {
    File classpathJar = new SharedFile(project).getEngineClasspathJar();
    
    if (!classpathJar.exists())
    {
      try
      {
        log.info("Creating a classpath jar for starting the engine");
        new ClasspathJar(classpathJar).createFileEntries(EngineClassLoaderFactory.getOsgiBootstrapClasspath(engineDirectory));
      }
      catch (Exception ex)
      {
        throw new RuntimeException(
                "Could not create engine classpath jar: '" + classpathJar.getAbsolutePath() + "'", ex);
      }
    }
    
    return classpathJar.getAbsolutePath();
  }
}
