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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;

/**
 * Copy maven dependencies to a specific folder.
 * 
 * @since 9.2.0
 */
@Mojo(name=MavenDependencyMojo.GOAL, requiresDependencyResolution=ResolutionScope.COMPILE)
public class MavenDependencyMojo extends AbstractProjectCompileMojo
{
  public static final String GOAL = "maven-dependency";
  
  /** 
   * Set to <code>true</code> to bypass the copy of <b>maven dependencies</b>.
   */
  @Parameter(property="ivy.mvn.dep.skip", defaultValue="false")
  boolean skipMvnDependency;
  
  int copied = 0;
  int ignored = 0;

  @Override
  protected void compile(MavenProjectBuilderProxy projectBuilder) throws Exception
  {
    if (skipMvnDependency)
    {
      return;
    }
    getLog().info("Copy maven dependencies...");
    
    var deps = getDependencies("jar");
    var mvnLibDir = Files.createDirectories(project.getBasedir().toPath().resolve("lib").resolve("mvn-deps"));
    deps.forEach(dep -> copyDependency(mvnLibDir, dep));
    
    getLog().info("Maven dependecies: " + copied + " copied, " + ignored + " ignored.");
  }

  private void copyDependency(Path mvnLibDir, File dep)
  {
    try
    {
      Files.copy(dep.toPath(), mvnLibDir.resolve(dep.getName()));
      getLog().info("Copied dependency: " + dep.getName());
      copied ++;
    }
    catch (FileAlreadyExistsException ex)
    {
      getLog().debug("Ignore dependecy '" + dep.getName() + "' as it already exists at: " + mvnLibDir);
      ignored ++;
    }
    catch (IOException ex)
    {
      getLog().warn("Couldn't copy depedency '" + dep +"' to: " + mvnLibDir, ex);
      ex.printStackTrace();
    }
  }
  
}
