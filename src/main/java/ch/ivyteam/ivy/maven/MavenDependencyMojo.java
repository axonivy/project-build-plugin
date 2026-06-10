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

package ch.ivyteam.ivy.maven;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import ch.ivyteam.ivy.maven.compile.AbstractProjectCompileMojo;
import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;
import ch.ivyteam.ivy.maven.util.MavenDependencies;

/**
 * Copy <a href="https://maven.apache.org/pom.html#Dependencies">maven
 * dependencies</a> to a specific folder.
 *
 * <p>
 * To reduce the size of your ivy archives, make sure that your dependencies are
 * configured correctly:
 * </p>
 * <ul>
 * <li>Mark test dependencies with the scope <b>test</b></li>
 * <li><a href="https://maven.apache.org/pom.html#exclusions">Exclude transient
 * dependencies</a> which are already delivered by the core</li>
 * </ul>
 *
 * @since 9.2.0
 */
@Mojo(name = MavenDependencyMojo.GOAL, requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM)
public class MavenDependencyMojo extends AbstractProjectCompileMojo {
  public static final String GOAL = "maven-dependency";

  /**
   * Set to <code>true</code> to bypass the copy of <b>maven dependencies</b>.
   */
  @Parameter(property = "ivy.mvn.dep.skip", defaultValue = "false")
  boolean skipMvnDependency;

  @Parameter(defaultValue = "${session}", readonly = true)
  private MavenSession session;

  @Override
  protected void engineExec(MavenProjectBuilderProxy projectBuilder) throws Exception {
    if (skipMvnDependency) {
      return;
    }
    getLog().info("Copy maven dependencies...");

    var deps = new MavenDependencies(project, session).localTransient();
    if (deps.isEmpty()) {
      getLog().info("No maven dependencies were found.");
      return;
    }
    var mvnLibDir = Files.createDirectories(project.getBasedir().toPath().resolve("lib").resolve("mvn-deps"));
    var copied = copyDependency(mvnLibDir, deps);

    getLog().info("Maven dependecies: " + copied + " copied.");
  }

  private int copyDependency(Path mvnLibDir, List<Path> deps) {
    var count = 0;
    for (var dep : deps) {
      try {
        Files.copy(dep, mvnLibDir.resolve(dep.getFileName().toString()));
        getLog().debug("Copied dependency: " + dep.getFileName());
        count++;
      } catch (FileAlreadyExistsException ex) {
        getLog().debug("Ignore dependecy '" + dep.getFileName() + "' as it already exists at: " + mvnLibDir);
      } catch (IOException ex) {
        getLog().warn("Couldn't copy depedency '" + deps + "' to: " + mvnLibDir, ex);
      }
    }
    return count;
  }

}
