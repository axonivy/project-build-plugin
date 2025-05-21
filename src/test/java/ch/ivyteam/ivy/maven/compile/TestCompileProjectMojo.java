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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.BaseEngineProjectMojoTest;
import ch.ivyteam.ivy.maven.log.LogCollector;
import ch.ivyteam.ivy.maven.util.PathUtils;

public class TestCompileProjectMojo extends BaseEngineProjectMojoTest {
  private CompileTestProjectMojo testMojo;

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;

  @Before
  public void setup() {
    System.setOut(new PrintStream(outContent));
  }

  @After
  public void restoreStreams() {
    System.setOut(originalOut);

  }

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

  @Test
  public void validateProcess() throws Exception {
    CompileProjectMojo mojo = compile.getMojo();

    Path project = mojo.project.getBasedir().toPath();
    var dataClassDir = project.resolve("src_dataClasses");
    var wsProcDir = project.resolve("src_wsproc");
    PathUtils.clean(wsProcDir);
    PathUtils.clean(dataClassDir);

    var ws = project.resolve("processes").resolve("myWebService.p.json");
    String wsJson = Files.readString(ws);
    var patched = StringUtils.replace(wsJson, "//TEMPLATE!!", "ivy.session.assignRole(null);");
    Files.writeString(ws, patched);

    mojo.buildApplicationDirectory = Files.createTempDirectory("MyBuildApplicationVald");
    mojo.execute();

    assertThat(outContent.toString())
        .contains("processes/myWebService.p.json /element=148CA74B16C580BF-ws0 : "
            + "Start code: Method assignRole of class ch.ivyteam.ivy.workflow.IWorkflowSession "
            + "is deprecated");

    var warning = outContent.toString().lines()
        .filter(l -> l.contains("/element=148CA74B16C580BF-ws0"))
        .findFirst().get();
    assertThat(warning)
        .as("WARNING prefix is streamlined with Maven CLI")
        .startsWith("[WARNING]");

    originalOut.print(outContent);
  }

}
