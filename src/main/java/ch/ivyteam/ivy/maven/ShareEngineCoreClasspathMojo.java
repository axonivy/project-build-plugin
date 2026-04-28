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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import ch.ivyteam.ivy.maven.engine.EngineClassLoaderFactory.OsgiDir;
import ch.ivyteam.ivy.maven.util.MavenProperties;

/**
 * Shares the Engine core classpath with the property:
 * <code>ivy.engine.core.classpath</code>.
 *
 * @since 6.2.0
 * @deprecated Sharing the classpath of the Axon Ivy Engine is no longer needed, because all
 *             dependencies are available as Maven dependencies.
 */
@Deprecated(since = "14.0.0")
@Mojo(name = ShareEngineCoreClasspathMojo.GOAL)
public class ShareEngineCoreClasspathMojo extends AbstractEngineMojo {

  private static final List<String> ENGINE_LIB_DIRECTORIES = Arrays.asList(
      OsgiDir.INSTALL_AREA + "/" + OsgiDir.LIB_BOOT,
      OsgiDir.PLUGINS,
      OsgiDir.INSTALL_AREA + "/configuration/org.eclipse.osgi", // unpacked jars from OSGI bundles
      "webapps" + File.separator + "ivy" + File.separator + "WEB-INF" + File.separator + "lib");

  @Deprecated
  public static final String GOAL = "share-engine-core-classpath";

  @Deprecated
  public static final String IVY_ENGINE_CORE_CLASSPATH_PROPERTY = "ivy.engine.core.classpath";

  @Deprecated
  @Parameter(property = "project", required = true, readonly = true)
  MavenProject project;

  @Deprecated
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    List<Path> ivyEngineClassPathFiles = getIvyEngineClassPathFiles(identifyAndGetEngineDirectory());
    String propertyValue = StringUtils.join(ivyEngineClassPathFiles, ",");
    MavenProperties properties = new MavenProperties(project, getLog());
    properties.setMavenProperty(IVY_ENGINE_CORE_CLASSPATH_PROPERTY, propertyValue);
  }

  private static List<Path> getIvyEngineClassPathFiles(Path engineDirectory) {
    if (engineDirectory == null) {
      return List.of();
    }

    var classPathFiles = new ArrayList<Path>();
    for (String libDirPath : ENGINE_LIB_DIRECTORIES) {
      var jarDir = engineDirectory.resolve(libDirPath);
      if (!Files.isDirectory(jarDir)) {
        continue;
      }
      try (var walker = Files.walk(jarDir)) {
        var jars = walker
            .filter(p -> p.getFileName().toString().endsWith(".jar"))
            .toList();
        classPathFiles.addAll(jars);
      } catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }
    return classPathFiles;
  }
}
