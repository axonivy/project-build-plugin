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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest;
import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.maven.util.PathUtils;

public class TestCompileProjectMojo extends BaseEngineProjectMojoTest {
  private CompileTestProjectMojo testMojo;

  @Rule
  public CompileMojoRule<CompileProjectMojo> compile = new CompileMojoRule<>(
      CompileProjectMojo.GOAL){
    @Override
    protected void before() throws Throwable {
      super.before();
      // use same project as first rule/mojo
      testMojo = (CompileTestProjectMojo) lookupConfiguredMojo(project, CompileTestProjectMojo.GOAL);
      configureMojo(testMojo);
    }
  };

  @Test
  public void buildWithExistingProject() throws Exception {
    CompileProjectMojo mojo = compile.getMojo();

    var dataClassDir = mojo.project.getBasedir().toPath().resolve("src_dataClasses");
    var wsProcDir = mojo.project.getBasedir().toPath().resolve("src_wsproc");
    var classDir = mojo.project.getBasedir().toPath().resolve("classes");
    PathUtils.clean(wsProcDir);
    PathUtils.clean(dataClassDir);

    mojo.buildApplicationDirectory = Files.createTempDirectory("MyBuildApplication");
    mojo.execute();

    assertThat(findFiles(dataClassDir, "java")).hasSize(2);
    assertThat(findFiles(wsProcDir, "java")).hasSize(1);

    assertThat(findFiles(classDir, "txt"))
        .as("classes directory must only be cleand by the builder before compilation if class files are found")
        .hasSize(1);
    assertThat(findFiles(classDir, "class"))
        .as("compiled classes must exist. but not contain any test class or old class files.")
        .hasSize(4);

    testMojo.execute();
    assertThat(findFiles(classDir, "class"))
        .as("compiled classes must contain test resources as well")
        .hasSize(5);
  }

  private static List<Path> findFiles(Path dir, String fileExtension) throws IOException {
    if (!Files.exists(dir)) {
      return List.of();
    }
    try (var stream = Files.walk(dir)) {
      return stream
          .filter(p -> p.getFileName().toString().endsWith("." + fileExtension))
          .toList();
    }
  }

  @Test
  public void compilerSettingsFile_notFoundWarnings() throws Exception {
    LogCollector log = new LogCollector();
    CompileProjectMojo mojo = compile.getMojo();
    mojo.setLog(log);

    mojo.compilerWarnings = false;
    mojo.compilerSettings = Path.of("path/to/oblivion");

    mojo.execute();
    assertThat(log.getWarnings().toString()).doesNotContain("Could not locate compiler settings file");

    mojo.compilerWarnings = true;
    mojo.execute();
    assertThat(log.getWarnings().toString()).contains("Could not locate compiler settings file");
  }
}
