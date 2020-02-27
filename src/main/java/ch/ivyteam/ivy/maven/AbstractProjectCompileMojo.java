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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory;
import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.MavenContext;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;
import ch.ivyteam.ivy.maven.engine.Slf4jSimpleEngineProperties;

public abstract class AbstractProjectCompileMojo extends AbstractEngineMojo
{
  @Parameter(property = "project", required = true, readonly = true)
  protected MavenProject project;
  
  /**
   * Home application where the project to build and its dependencies will be temporary deployed. 
   */
  @Parameter(defaultValue = "${project.build.directory}/ivyBuildApp")
  protected File buildApplicationDirectory;
  
  /** 
   * Specifies the default encoding for all source files. By default this is the charset of the JVM according to {@link Charset#defaultCharset()}.
   * You may set it to another value like 'UTF-8'.
   * @since 6.3.1
   */
  @Parameter(property="ivy.compiler.encoding")
  private String encoding;

  /** 
   * Defines the timeout how long to wait for an engine start to compile.
   * @since 7.4.0
   */
  @Parameter(property = "ivy.compiler.engine.start.timeout", defaultValue = "60")
  private int timeoutEngineStartInSeconds;

  /**
   * Set to <code>false</code> to disable compilation warnings.
   * @since 8.0.3
   */
  @Parameter(property = "ivy.compiler.warnings", defaultValue = "true")
  protected boolean compilerWarnings;

  /**
   * Define a compiler settings file to configure compilation warnings.
   * Such file can be created in the Designer: <i>Window - Preferences - Java - Compiler - Errors/Warnings</i>,
   * the corresponding file can be found in:
   * <i>designer-workspace/.metadata/.plugins/org.eclipse.core.runtime/.settings/org.eclipse.jdt.core.prefs</i>
   * <br>
   * If left empty the plugin will try to load the project specific settings file <i>project/.settings/org.eclipse.jdt.core.prefs</i>
   * <br>
   * These settings are only active when {@link AbstractProjectCompileMojo#compilerWarnings} is set to <code>true</code>.
   * @since 8.0.3
   */
  @Parameter(property = "ivy.compiler.settings", defaultValue = ".settings/org.eclipse.jdt.core.prefs")
  protected File compilerSettings;

  @Component
  private RepositorySystem repository;
  
  @Parameter(defaultValue = "${localRepository}")
  protected ArtifactRepository localRepository;
  
  private static MavenProjectBuilderProxy builder;

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException
  {
    Slf4jSimpleEngineProperties.install();
    try
    {
      compile(getMavenProjectBuilder());
    }
    catch (Exception ex)
    {
      throw new MojoExecutionException("Failed to compile project '"+project.getBasedir()+"'.", ex);
    }
    finally
    {
      Slf4jSimpleEngineProperties.reset();
    }
  }
  
  protected abstract void compile(MavenProjectBuilderProxy projectBuilder) throws Exception;

  private MavenProjectBuilderProxy getMavenProjectBuilder() throws Exception
  {
    EngineClassLoaderFactory classLoaderFactory = getEngineClassloaderFactory();

    File engineDir = identifyAndGetEngineDirectory();
    if (builder == null)
    {
      builder = new MavenProjectBuilderProxy(
              classLoaderFactory,
              buildApplicationDirectory,
              engineDir,
              getLog(),
              timeoutEngineStartInSeconds);
    }
    classLoaderFactory.writeEngineClasspathJar(engineDir);
    // share engine directory as property for custom follow up plugins:
    if (engineDir != null)
    {
      project.getProperties().put(AbstractEngineMojo.ENGINE_DIRECTORY_PROPERTY, engineDir.getAbsolutePath());
    }
    return builder;
  }

  private EngineClassLoaderFactory getEngineClassloaderFactory()
  {
    MavenContext context = new EngineClassLoaderFactory.MavenContext(
            repository, localRepository, project, getLog());
    return new EngineClassLoaderFactory(context);
  }

  protected Map<String, Object> getOptions()
  {
    Map<String, Object> options = new HashMap<>();
    options.put(MavenProjectBuilderProxy.Options.TEST_SOURCE_DIR, project.getBuild().getTestSourceDirectory());
    options.put(MavenProjectBuilderProxy.Options.COMPILE_CLASSPATH, getDependencyClasspath());
    options.put(MavenProjectBuilderProxy.Options.SOURCE_ENCODING, encoding);
    options.put(MavenProjectBuilderProxy.Options.WARNINGS_ENABLED, Boolean.toString(compilerWarnings));
    options.put(MavenProjectBuilderProxy.Options.JDT_SETTINGS_FILE, getCompilerSettings());
    return options;
  }

  private String getDependencyClasspath()
  {
    return StringUtils.join(getDependencies("jar").stream()
            .map(jar -> jar.getAbsolutePath())
            .collect(Collectors.toList()), File.pathSeparatorChar);
  }

  private File getCompilerSettings()
  {
    if (compilerSettings.exists())
    {
      return compilerSettings;
    }
    else if (compilerWarnings)
    {
      getLog().warn("Could not locate compiler settings file: " + compilerSettings + " continuing with default compiler settings");
    }
    return null;
  }

  protected final List<File> getDependencies(String type)
  {
    Set<org.apache.maven.artifact.Artifact> dependencies = project.getArtifacts();
    if (dependencies == null)
    {
      return Collections.emptyList();
    }
    
    List<File> dependentIars = new ArrayList<>();
    for(org.apache.maven.artifact.Artifact artifact : dependencies)
    {
      if (artifact.getType().equals(type))
      {
        dependentIars.add(artifact.getFile());
      }
    }
    return dependentIars;
  }

}