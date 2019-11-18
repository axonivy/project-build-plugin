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
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy.Options;
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
   * Logs warnings found by the compiler.
   * @since 8.1.0
   */
  @Parameter(property="ivy.compiler.warnings.enabled", defaultValue = "false")
  private boolean compilerWarnings;

  /** 
   * Properties file to configure JDT compiler severities (info/warn/error).
   * A valid properties file can be created using the Axon.ivy Designer:
   * <ol>
   *    <li>Open the 'Package Explorer': Windows > Show View > Other > Package Explorer</li>
   *    <li>In Package Explorer: right click on an IvyProject > Properties</li>
   *    <li>Navigate to Java > Compiler > Errors/Warnings</li>
   *    <li>Enable project specific settings</li>
   *    <li>Configure objects to analyze and it's report severity</li>
   *    <li>Apply and close preferences dialog afterwards</li>
   *    <li>Now the settings are persisted in '.settings/org.eclipse.jdt.prefs' of the project</li>
   *    <li>Copy the org.eclipse.jdt.prefs to a location of your choice and pass it's path to this property</li>
   * </ol>
   * 
   * @since 8.1.0
   */
  @Parameter(property="ivy.compiler.properties.file")
  private File compilerProperties;

  /** 
   * Defines the timeout how long to wait for an engine start to compile.
   * @since 7.4.0
   */
  @Parameter(property = "ivy.compiler.engine.start.timeout", defaultValue = "60")
  private int timeoutEngineStartInSeconds;

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

  public final EngineClassLoaderFactory getEngineClassloaderFactory()
  {
    MavenContext context = new EngineClassLoaderFactory.MavenContext(
            repository, localRepository, project, getLog());
    return new EngineClassLoaderFactory(context);
  }

  protected Map<String, String> getOptions()
  {
    Map<String, String> options = new HashMap<>();
    options.put(Options.TEST_SOURCE_DIR, project.getBuild().getTestSourceDirectory());
    options.put(Options.COMPILE_CLASSPATH, getDependencyClasspath());
    options.put(Options.SOURCE_ENCODING, encoding);
    options.put(Options.WARNINGS_ENABLED, String.valueOf(compilerWarnings));
    if (compilerProperties != null)
    {
      options.put(Options.SEVERITY_PROPERTIES, compilerProperties.getPath());
    }
    return options;
  }

  private String getDependencyClasspath()
  {
    return StringUtils.join(getDependencies("jar").stream()
            .map(jar -> jar.getAbsolutePath())
            .collect(Collectors.toList()), File.pathSeparatorChar);
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