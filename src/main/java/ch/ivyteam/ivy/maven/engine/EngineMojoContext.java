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

package ch.ivyteam.ivy.maven.engine;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.util.ClasspathJar;
import ch.ivyteam.ivy.maven.util.MavenProperties;
import ch.ivyteam.ivy.maven.util.SharedFile;

public class EngineMojoContext {

  public final Path engineDirectory;
  public final MavenProject project;
  public final Log log;
  public final EngineVmOptions vmOptions;
  public final Path engineClasspathJarPath;
  public final MavenProperties properties;
  public final Path engineLogFile;
  public final Integer timeoutInSeconds;

  public EngineMojoContext(Path engineDirectory, MavenProject project, Log log, Path engineLogFile,
          EngineVmOptions vmOptions, Integer timeoutInSeconds) {
    this.engineDirectory = engineDirectory;
    this.project = project;
    this.log = log;
    this.engineLogFile = engineLogFile;
    this.vmOptions = vmOptions;
    this.timeoutInSeconds = timeoutInSeconds;

    this.properties = new MavenProperties(project, log);
    this.engineClasspathJarPath = setupEngineClasspathJarIfNotExists();

    if (!Files.exists(engineClasspathJarPath)) {
      throw new RuntimeException("Engine ClasspathJar " + engineClasspathJarPath + " does not exist.");
    }
    if (!Files.exists(engineDirectory)) {
      throw new RuntimeException("Engine Directory " + engineDirectory + " does not exist.");
    }
  }

  private Path setupEngineClasspathJarIfNotExists() {
    var classpathJar = new SharedFile(project).getEngineOSGiBootClasspathJar().toAbsolutePath();
    if (!Files.exists(classpathJar)) {
      try {
        log.info("Creating a classpath jar for starting the engine");
        new ClasspathJar(classpathJar.toFile())
                .createFileEntries(EngineClassLoaderFactory.getOsgiBootstrapClasspath(engineDirectory.toFile()));
      } catch (Exception ex) {
        throw new RuntimeException(
                "Could not create engine classpath jar: '" + classpathJar + "'", ex);
      }
    }
    return classpathJar;
  }
}
