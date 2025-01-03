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
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import ch.ivyteam.ivy.maven.engine.MavenProjectBuilderProxy;
import ch.ivyteam.ivy.maven.util.ClasspathJar;
import ch.ivyteam.ivy.maven.util.CompilerResult;
import ch.ivyteam.ivy.maven.util.SharedFile;

/**
 * Compiles the test sources.
 *
 * @author Reguel Wermelinger
 * @since 6.1.0
 */
@Mojo(name = CompileTestProjectMojo.GOAL, requiresDependencyResolution = ResolutionScope.TEST)
public class CompileTestProjectMojo extends AbstractProjectCompileMojo {
  public static final String GOAL = "test-compile";

  /** Set to <code>true</code> to bypass the compilation of test sources. */
  @Parameter(property = "maven.test.skip", defaultValue = "false")
  boolean skipTest;

  @Override
  protected void engineExec(MavenProjectBuilderProxy projectBuilder) throws Exception {
    if (skipTest) {
      return;
    }

    getLog().info("Compiling test sources...");
    Map<String, Object> result = projectBuilder.testCompile(project.getBasedir(), getDependencyIarJars(), getOptions());
    CompilerResult.store(result, project);
  }

  /**
   * @return persistent IAR-JARs from {@link CompileProjectMojo}.
   */
  private List<File> getDependencyIarJars() {
    var iarJarClasspath = new SharedFile(project).getIarDependencyClasspathJar();
    if (!Files.exists(iarJarClasspath)) {
      return Collections.emptyList();
    }
    return new ClasspathJar(iarJarClasspath).getFiles();
  }
}
