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

import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Delete copied maven dependencies in the lib/mvn-deps folder.
 * 
 * @since 9.2.0
 */
@Mojo(name=MavenDependencyCleanupMojo.GOAL)
public class MavenDependencyCleanupMojo extends AbstractMojo
{
  public static final String GOAL = "maven-dependency-cleanup";
  
  @Parameter(property = "project", required = true, readonly = true) 
  MavenProject project;
  
  /** 
   * Set to <code>true</code> to bypass the deletion of <b>maven dependencies</b> copied by the {@value MavenDependencyMojo#GOAL} step.
   */
  @Parameter(property="ivy.mvn.dep.cleanup.skip", defaultValue="false")
  boolean skipMvnDependencyCleanup;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    if (skipMvnDependencyCleanup)
    {
      return;
    }
    var mvnLibDir = project.getBasedir().toPath().resolve("lib").resolve("mvn-deps");
    getLog().info("Deleting " + mvnLibDir.toString());
    try
    {
      FileUtils.deleteDirectory(mvnLibDir.toFile());
    }
    catch (IOException ex)
    {
      getLog().warn("Couldn't delete: " + mvnLibDir.toString(), ex);
    }
  }

}
