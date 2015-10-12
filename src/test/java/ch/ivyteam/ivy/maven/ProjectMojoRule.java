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
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.project.MavenProject;

/**
 * Simple rule that can provide a real set-up MOJO that works on a copy of the given projectDirectory.
 * This simplifies TEST dramatically whenever your MOJO relies on real Maven Models like (Project, Artifact, ...)
 * 
 * @author Reguel Wermelinger
 * @since 03.10.2014
 * @param <T>
 */
public class ProjectMojoRule<T extends Mojo> extends MojoRule
{
  protected File projectDir;
  private T mojo;
  private String mojoName;
  private File templateProjectDir;
  
  public ProjectMojoRule(File srcDir, String mojoName)
  {
    this.templateProjectDir = srcDir;
    this.mojoName = mojoName;
  }
  
  @Override
  @SuppressWarnings("unchecked")
  protected void before() throws Throwable 
  {
    projectDir = Files.createTempDirectory("MyBaseProject").toFile();
    FileUtils.copyDirectory(templateProjectDir, projectDir);
    MavenProject project = readMavenProject(projectDir);
    mojo = (T) lookupConfiguredMojo(project, mojoName);
  }
  
  @Override
  protected void after() 
  {
    try
    {
      FileUtils.deleteDirectory(projectDir);
    }
    catch (IOException ex)
    {
      throw new RuntimeException(ex);
    }  
  }

  public T getMojo()
  {
    return mojo;
  }
}