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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.util.ClasspathJar;
import ch.ivyteam.ivy.maven.util.CompilerResult;
import ch.ivyteam.ivy.maven.util.MavenProperties;
import ch.ivyteam.ivy.maven.util.SharedFile;

/**
 * Shares the classpath of the built ivy project and it's engine as public property 
 * and tries to auto-configure maven-surefire-plugin to use this classpath.
 * 
 * @author Reguel Wermelinger
 * @since 6.0.2
 */
@Mojo(name = SetupIvyTestPropertiesMojo.GOAL)
public class SetupIvyTestPropertiesMojo extends AbstractMojo
{
  public static final String GOAL = "ivy-test-properties";
  
  public static final String IVY_ENGINE_CLASSPATH_PROPERTY = "ivy.engine.classpath";
  public static final String IVY_PROJECT_IAR_CLASSPATH_PROPERTY = "ivy.project.iar.classpath";
  public static final String MAVEN_TEST_ADDITIONAL_CLASSPATH_PROPERTY = "maven.test.additionalClasspath";

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  /** 
   * Set to <code>true</code> to bypass property set-up. 
   * @since 6.1.0
   */
  @Parameter(defaultValue="false", property="maven.test.skip")
  boolean skipTest;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    if (skipTest)
    {
      return;
    }
    
    MavenProperties properties = new MavenProperties(project, getLog());
    setIvyProperties(properties);
    configureMavenTestProperties(properties);
    setTestOutputDirectory();
  }

  private void setIvyProperties(MavenProperties properties)
  {
    SharedFile shared = new SharedFile(project);
    File engineCp = shared.getEngineClasspathJar();
    if (engineCp.exists())
    {
      properties.setMavenProperty(IVY_ENGINE_CLASSPATH_PROPERTY, getClasspath(engineCp));
    }

    File iarCp = shared.getIarDependencyClasspathJar();
    if (iarCp.exists())
    {
      properties.setMavenProperty(IVY_PROJECT_IAR_CLASSPATH_PROPERTY, getClasspath(iarCp));
    }
  }
  
  /**
   * defines properties that are interpreted by maven-surefire.
   */
  private void configureMavenTestProperties(MavenProperties properties)
  {
    List<String> IVY_PROPS = List.of(IVY_ENGINE_CLASSPATH_PROPERTY, IVY_PROJECT_IAR_CLASSPATH_PROPERTY);
    String surefireClasspath = IVY_PROPS.stream().collect(Collectors.joining(",", "${", "}"));
    properties.setMavenProperty(MAVEN_TEST_ADDITIONAL_CLASSPATH_PROPERTY, surefireClasspath);
  }

  private void setTestOutputDirectory()
  {
    try
    {
      String testOutputDirectory = CompilerResult.load(project).getTestOutputDirectory();
      if (testOutputDirectory != null)
      {
        project.getBuild().setTestOutputDirectory(
                new File(project.getBasedir(), testOutputDirectory).getAbsolutePath());
      }
    }
    catch (IOException ex)
    {
      getLog().warn("Failed to set up ${project.build.testOutputDirectory}", ex);
    }
  }

  private static String getClasspath(File jar)
  {
    return new ClasspathJar(jar).getClasspathFiles();
  }

}
