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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.util.PathUtils;

/**
 * Simple rule that can provide a real set-up MOJO that works on a copy of the
 * given projectDirectory. This simplifies TEST dramatically whenever your MOJO
 * relies on real Maven Models like (Project, Artifact, ...)
 *
 * @author Reguel Wermelinger
 * @since 03.10.2014
 * @param <T>
 */
public class ProjectMojoRule<T extends Mojo> extends MojoRule {

  protected Path projectDir;
  private T mojo;
  private String mojoName;
  private Path templateProjectDir;
  public MavenProject project;

  public ProjectMojoRule(Path srcDir, String mojoName) {
    this.templateProjectDir = srcDir;
    this.mojoName = mojoName;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void before() throws Throwable {
    projectDir = Files.createTempDirectory("MyBaseProject");
    copyDirectory(templateProjectDir, projectDir);
    project = readMavenProject(projectDir.toFile());
    mojo = (T) lookupConfiguredMojo(project, mojoName);
  }

  @Override
  protected void after() {
    PathUtils.delete(projectDir);
  }

  public T getMojo() {
    return mojo;
  }

  private static void copyDirectory(Path source, Path target) {
    try (var walker = Files.walk(source)) {
      walker.forEach(fileToCopy -> {
        if (fileToCopy.equals(source)) {
          return;
        }
        var destination = source.relativize(fileToCopy);
        var fileToMove = target.resolve(destination);

        try {
          var dirToMove = fileToMove.getParent();
          if (!Files.exists(dirToMove)) {
            Files.createDirectories(dirToMove);
          }
          Files.copy(fileToCopy, fileToMove);
        } catch (IOException ex) {
          throw new UncheckedIOException(ex);
        }
      });
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}