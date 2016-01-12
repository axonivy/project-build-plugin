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

package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
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
import ch.ivyteam.ivy.maven.util.ClasspathJar;
import ch.ivyteam.ivy.maven.util.SharedFile;

/**
 * Compiles an ivy Project with an ivyEngine.
 * 
 * @author Reguel Wermelinger
 * @since 6.0.0
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
  
  static MavenProjectBuilderProxy builder;
  static File engineAppDirInUse;
  
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
      getMavenProjectBuilder().compile(project.getBasedir(), resolveIarDependencies());
      writeDependencyIarJar();
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("Failed to compile project '"+project.getBasedir()+"'.", ex);
    }
  }

  private MavenProjectBuilderProxy getMavenProjectBuilder() throws Exception
  {
    URLClassLoader engineClassloader = getEngineClassloader(); // always instantiate -> write classpath jar!
    if (builder == null)
    {
      builder = new MavenProjectBuilderProxy(engineClassloader, buildApplicationDirectory, getEngineDirectory());
      engineAppDirInUse = buildApplicationDirectory;
    }
    return builder;
  }

  URLClassLoader getEngineClassloader() throws IOException
  {
    MavenContext context = new EngineClassLoaderFactory.MavenContext(
            repository, localRepository, project, getLog());
    EngineClassLoaderFactory classLoaderFactory = new EngineClassLoaderFactory(context);
    return classLoaderFactory.createEngineClassLoader(getEngineDirectory());
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

  private void writeDependencyIarJar() throws IOException
  {
    if (!engineAppDirInUse.isDirectory())
    { // project has no dependencies
      return;
    }
    Collection<File> iarJarDepenencies = FileUtils.listFiles(engineAppDirInUse, new String[]{"jar"}, true);
    if (iarJarDepenencies == null)
    { // old engine which does not return it's dependencies
      return;
    }
    File jar = new SharedFile(project).getIarDependencyClasspathJar();
    new ClasspathJar(jar).createFileEntries(iarJarDepenencies);
  }

}
