/*
 * Copyright (C) 2015 AXON IVY AG
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
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory;
import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.MavenContext;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;

/**
 * Compiles an ivy Project with an ivyEngine.
 * 
 * @author Reguel Wermelinger
 * @since 04.11.2014
 */
@Mojo(name=CompileProjectMojo.GOAL, requiresDependencyResolution=ResolutionScope.COMPILE)
public class CompileProjectMojo extends AbstractEngineMojo
{
  public static final String GOAL = "compileProject";
  
  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;
  
  /**
   * Home application where the project to build and its dependencies will be temporary deployed. 
   */
  @Parameter(defaultValue = "${project.build.directory}/ivyBuildApp")
  File buildApplicationDirectory;
  
  @Component
  private RepositorySystem repository;
  
  @Parameter(defaultValue="${localRepository}")
  ArtifactRepository localRepository;
  
  private static MavenProjectBuilderProxy builder;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    getLog().info("Compiling ivy Project...");
    compileProject();
  }

  private void compileProject() throws MojoExecutionException
  {
    try
    {
      getMavenProjectBuilder().execute(project.getBasedir(), resolveIarDependencies(), getEngineDirectory().getAbsoluteFile());
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("Failed to compile project '"+project.getBasedir()+"'.", ex);
    }
  }
  
  private MavenProjectBuilderProxy getMavenProjectBuilder() throws Exception
  {
    if (builder == null)
    {
      MavenContext context = new EngineClassLoaderFactory.MavenContext(repository, localRepository, getLog());
      EngineClassLoaderFactory classLoaderFactory = new EngineClassLoaderFactory(context);
      URLClassLoader classLoader = classLoaderFactory.createEngineClassLoader(getEngineDirectory());
      builder = new MavenProjectBuilderProxy(classLoader, buildApplicationDirectory);
      return builder;
    }
    return builder;
  }
  
  private List<File> resolveIarDependencies()
  {
    Set<org.apache.maven.artifact.Artifact> dependencies = project.getArtifacts();
    if (dependencies == null)
    {
      return Collections.emptyList();
    }
    
    List<File> dependentIars = new ArrayList<>();
    for(org.apache.maven.artifact.Artifact artifact : dependencies)
    {
      if (artifact.getType().equals("iar"))
      {
        dependentIars.add(artifact.getFile());
      }
    }
    return dependentIars;
  }

}
