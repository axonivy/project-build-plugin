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

package ch.ivyteam.ivy.maven.compile;

import java.io.File;
import java.nio.file.Path;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;

import ch.ivyteam.ivy.maven.AbstractEngineMojo;
import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory;
import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.MavenContext;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;
import ch.ivyteam.ivy.maven.engine.Slf4jSimpleEngineProperties;

@SuppressWarnings("deprecation")
public abstract class AbstractEngineInstanceMojo extends AbstractEngineMojo {
  @Parameter(property = "project", required = true, readonly = true)
  public MavenProject project;

  /**
   * Home application where the project to build and its dependencies will be
   * temporary deployed.
   */
  @Parameter(defaultValue = "${project.build.directory}/ivyBuildApp")
  protected Path buildApplicationDirectory;

  /**
   * Defines the timeout how long to wait for an engine start to compile.
   * @since 7.4.0
   */
  @Parameter(property = "ivy.compiler.engine.start.timeout", defaultValue = "60")
  private int timeoutEngineStartInSeconds;

  @Component
  private RepositorySystem repository;

  @Parameter(defaultValue = "${localRepository}")
  public ArtifactRepository localRepository;

  private static MavenProjectBuilderProxy builder;

  public AbstractEngineInstanceMojo() {}

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    Slf4jSimpleEngineProperties.install();
    try {
      engineExec(getMavenProjectBuilder());
    } catch (Exception ex) {
      throw new MojoExecutionException("Failed to compile project '" + project.getBasedir() + "'.", ex);
    } finally {
      Slf4jSimpleEngineProperties.reset();
    }
  }

  protected abstract void engineExec(MavenProjectBuilderProxy projectBuilder) throws Exception;

  protected MavenProjectBuilderProxy getMavenProjectBuilder() throws Exception {
    EngineClassLoaderFactory classLoaderFactory = getEngineClassloaderFactory();

    var engineDir = identifyAndGetEngineDirectory();
    if (builder == null) {
      builder = new MavenProjectBuilderProxy(
          classLoaderFactory,
          toFile(buildApplicationDirectory),
          toFile(engineDir),
          getLog(),
          timeoutEngineStartInSeconds);
    }
    classLoaderFactory.writeEngineClasspathJar(engineDir);
    // share engine directory as property for custom follow up plugins:
    if (engineDir != null) {
      project.getProperties().put(AbstractEngineMojo.ENGINE_DIRECTORY_PROPERTY, engineDir.toAbsolutePath().toString());
    }
    return builder;
  }

  private File toFile(Path path) {
    return path == null ? null : path.toFile();
  }

  public final EngineClassLoaderFactory getEngineClassloaderFactory() {
    MavenContext context = new EngineClassLoaderFactory.MavenContext(
        repository, localRepository, project, getLog());
    return new EngineClassLoaderFactory(context);
  }

}
