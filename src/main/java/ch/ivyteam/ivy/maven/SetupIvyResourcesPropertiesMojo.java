/*
 * Copyright (C) 2022 Axon Ivy AG
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.util.MavenProperties;

/**
 * Set the "project.build.sourceEncoding" property to "UTF-8" for the
 * maven-resource plugin, if not manually defined.
 *
 * @since 9.4.0
 */
@Mojo(name = SetupIvyResourcesPropertiesMojo.GOAL,
    threadSafe = true,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public class SetupIvyResourcesPropertiesMojo extends AbstractMojo {
  public static final String GOAL = "ivy-resources-properties";

  public static final String PROJECT_BUILD_SOURCEENCODING = "project.build.sourceEncoding";

  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    var properties = new MavenProperties(project, getLog());
    var sourceEncoding = properties.get(PROJECT_BUILD_SOURCEENCODING);
    if (sourceEncoding != null) {
      getLog().info("'" + PROJECT_BUILD_SOURCEENCODING + "' is already set to '" +
          sourceEncoding + "', so it will not be overwritten to UTF-8");
      return;
    }
    getLog().info("Set '" + PROJECT_BUILD_SOURCEENCODING + "' to UTF-8");
    properties.setMavenProperty(PROJECT_BUILD_SOURCEENCODING, "UTF-8");
  }
}
