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

package ch.ivyteam.ivy.maven.util;

import java.io.File;

import org.apache.maven.project.MavenProject;

/**
 * A file that is used by multiple Mojos/Goals during the build lifecycle.
 * 
 * @since 6.0.3
 */
public class SharedFile
{
  private File targetDir;
  
  public SharedFile(MavenProject project)
  {
    targetDir = new File(project.getBuild().getDirectory());
  }
  
  public File getEngineClasspathJar()
  {
    return new File(targetDir, "ivy.engine.classpath.jar");
  }

  public File getIarDependencyClasspathJar()
  {
    return new File(targetDir, "ivy.project.dependency.classpath.jar");
  }
  
}
